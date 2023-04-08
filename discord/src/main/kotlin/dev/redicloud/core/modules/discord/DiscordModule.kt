@file:OptIn(PrivilegedIntent::class)

package dev.redicloud.core.modules.discord

import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.builder.kord.KordBuilder
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.embed
import dev.redicloud.api.ICoreAPI
import dev.redicloud.api.module.Module
import dev.redicloud.api.utils.networkDomain
import dev.redicloud.api.utils.networkName
import dev.redicloud.core.modules.discord.commands.*
import dev.redicloud.core.modules.discord.data.DiscordConfig
import dev.redicloud.core.modules.discord.data.chat.respondEphemeral
import dev.redicloud.core.modules.discord.part.parts
import dev.redicloud.core.modules.discord.util.commands.*
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign
import dev.redicloud.core.modules.discord.util.snowflake
import io.ktor.http.cio.*
import io.ktor.http.content.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("unused")
class DiscordModule : Module() {

    companion object {
        lateinit var INSTANCE: DiscordModule
    }

    var configCommands: MutableMap<Snowflake, InputCommandBuilder> = mutableMapOf()

    override fun onLoad(api: ICoreAPI) = runBlocking {
        INSTANCE = this@DiscordModule
        coreAPI = api
        configManager = api.configManager
        config = if (!configManager.exists("discord:bot")) {
            val discordConfig = DiscordConfig()
            configManager.createConfig(discordConfig)
            discordConfig
        } else {
            configManager.getConfig("discord:bot", DiscordConfig::class.java)
        }
    }

    override fun onEnable(api: ICoreAPI) {
        kordScope.launch {
            kord = Kord(System.getenv("DISCORD_BOT_TOKEN")) {
                stackTraceRecovery = true
                setupCache()
            }

            kord.on<ReadyEvent> {
                mainGuild = kord.getGuildOrThrow(config.publicDiscordID.snowflake)
                teamGuild = kord.getGuildOrThrow(config.teamDiscordID.snowflake)
                logChannel = kord.getChannelOf(config.logChannel.snowflake) ?: throw IllegalStateException(
                    "Log channel not found"
                )

                loadConfigCommand()

                kord.editPresence {
                    status = PresenceStatus.Online
                    playing("on $networkDomain")
                }

                println("Enabling discord module part:")
                parts.forEach {
                    it.init()
                    it.commands.forEach { command -> command.create() }
                    println("${it.name} enabled")
                }

                configCommands.forEach { it.value.create() }

                COMMANDS.addAll(mutableListOf(StopCommand(), UpTimeCommand()))

                COMMANDS.forEach { it.create() }

                notifyStart()
            }

            launch {
                kord.login {
                    intents += Intents.all
                }
            }

        }
    }

    override fun onDisable(api: ICoreAPI) = runBlocking {
        try {
            notifyStop()
            kord.logout()
        } catch (_: UninitializedPropertyAccessException) {
        }
    }

    private fun KordBuilder.setupCache() = cache {
        users { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        messages { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 10000))
        }

        members { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.none())
        }

        guilds { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        roles { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        channels { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        voiceState { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        emojis { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }
    }

    private suspend fun loadConfigCommand() {
        kord.guilds.collect {
            configCommands[it.id] = inputCommand("config", it.id, "Edit discord bot configs") {
                permissions = Permissions(Permission.All)
                group("test", "Test command") {
                    subCommand("embed", "Test embed") {
                        message("test", "This is a test embed") {
                            required = true
                        }
                        perform(this@group, this) {
                            val message = interaction.command.messages["test"]
                            if (message != null) {
                                ioScope.launch {
                                    interaction.respondEphemeral(message, interaction.user, mutableMapOf())
                                }
                            } else {
                                ioScope.launch {
                                    interaction.respondEphemeral {
                                        content = "Message not found:\n```${interaction.command.strings["test"]}```"
                                    }
                                }
                            }
                        }
                    }
                }
                group("general", "General configurations for the bot") {
                    subCommand("mainguild", "Edit the main guild") {
                        string("id", "The id of the main guild") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            val id = this.interaction.command.strings["id"]!!
                            ioScope.launch {
                                val guild = kord.getGuildOrNull(id.snowflake)
                                if (guild == null) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Error | $networkName"
                                            description = "The guild with the id $id could not be found"
                                            color = Color(255, 0, 0)
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                mainGuild = guild
                                config.publicDiscordID = id
                                configManager.saveConfig(config)
                                logChannel.createMessage {
                                    embed {
                                        title = "Info | $networkName"
                                        description =
                                            "The main guild was changed to ${guild.name} by ${interaction.user.asUser().mention}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The main guild was changed to ${guild.name}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }

                    subCommand("teamguild", "Edit the team guild") {
                        string("id", "The id of the team guild") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            val id = this.interaction.command.strings["id"]!!
                            ioScope.launch {
                                val guild = kord.getGuildOrNull(id.snowflake)
                                if (guild == null) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Error | $networkName"
                                            description = "The guild with the id $id could not be found"
                                            color = Color(255, 0, 0)
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                teamGuild = guild
                                config.teamDiscordID = id
                                configManager.saveConfig(config)
                                logChannel.createMessage {
                                    embed {
                                        title = "Info | $networkName"
                                        description =
                                            "The team guild has been changed to ${guild.name} by ${interaction.user.asUser().mention}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description =
                                            "The team guild was changed to ${guild.name} by ${interaction.user.asUser().mention}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }

                    subCommand("logchannel", "Edit the log channel") {
                        channel("channel", "The log channel") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                configManager.saveConfig(config)
                                logChannel.createMessage {
                                    embed {
                                        title = "Info | $networkName"
                                        description =
                                            "The log channel has been changed to ${channel.mention} by ${interaction.user.asUser().mention}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The log channel has been changed to ${channel.mention}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                logChannel = channel.asChannelOf()
                                logChannel.createMessage {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "This is now the log channel!"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
