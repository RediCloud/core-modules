@file:OptIn(DelicateCoroutinesApi::class, PrivilegedIntent::class)

package net.dustrean.modules.discord

import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.builder.kord.KordBuilder
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.embed
import io.ktor.http.cio.*
import io.ktor.http.content.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dustrean.api.ICoreAPI
import net.dustrean.api.module.Module
import net.dustrean.modules.discord.data.DiscordConfig
import net.dustrean.modules.discord.part.parts
import net.dustrean.modules.discord.util.commands.InputCommandBuilder
import net.dustrean.modules.discord.util.commands.inputCommand
import net.dustrean.modules.discord.util.snowflake

@Suppress("unused")
class DiscordModuleMain : Module() {

    companion object {
        var editCommands: MutableMap<Snowflake, InputCommandBuilder> = mutableMapOf()
    }

    override fun onLoad(api: ICoreAPI) = runBlocking {
        coreAPI = api
        configManager = api.getConfigManager()
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
                logChannel = kord.getChannelOf<TextChannel>(config.logChannel.snowflake) ?: throw IllegalStateException(
                    "Log channel not found"
                )

                loadEditCommand()

                kord.editPresence {
                    status = PresenceStatus.Online
                    playing("on dustrean.net")
                }

                println("Enabling discord module parts:")
                parts.forEach {
                    it.init()
                    it.commands.forEach { command -> command.create() }
                    println("${it.name} enabled")
                }

                editCommands.forEach { it.value.create() }
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

    private suspend fun loadEditCommand() {
        kord.guilds.collect {
            editCommands[it.id] = inputCommand("edit", it.id, "Edit discord bot configs") {
                permissions = Permissions(Permission.All)
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
                                            title = "Error | DustreanNET"
                                            description = "The guild with the id $id could not be found"
                                            color = Color(255, 0, 0)
                                        }
                                    }
                                    return@launch
                                }
                                config.publicDiscordID = id
                                configManager.saveConfig(config)
                                logChannel.createMessage {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description =
                                            "The main guild was changed to ${guild.name} by ${interaction.user.asUser().mention}"
                                    }
                                }
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The main guild was changed to ${guild.name}"
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
                                            title = "Error | DustreanNET"
                                            description = "The guild with the id $id could not be found"
                                            color = Color(255, 0, 0)
                                        }
                                    }
                                    return@launch
                                }
                                config.teamDiscordID = id
                                configManager.saveConfig(config)
                                logChannel.createMessage {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description =
                                            "The team guild has been changed to ${guild.name} by ${interaction.user.asUser().mention}"
                                    }
                                }
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description =
                                            "The team guild was changed to ${guild.name} by ${interaction.user.asUser().mention}"
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
                                        title = "Info | DustreanNET"
                                        description =
                                            "The log channel has been changed to ${channel.mention} by ${interaction.user.asUser().mention}"
                                    }
                                }
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The log channel has been changed to ${channel.mention}"
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
