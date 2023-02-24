package dev.redicloud.core.modules.discord.part.impl.rule

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.*
import dev.kord.core.event.channel.ChannelCreateEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.role
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.launch
import dev.redicloud.core.modules.discord.*
import dev.redicloud.core.modules.discord.data.chat.createMessage
import dev.redicloud.core.modules.discord.data.chat.emoji
import dev.redicloud.core.modules.discord.part.DiscordModulePart
import dev.redicloud.core.modules.discord.util.commands.CommandBuilder
import dev.redicloud.core.modules.discord.util.commands.message
import dev.redicloud.core.modules.discord.util.commands.messages
import dev.redicloud.core.modules.discord.util.interactions.button
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign
import dev.redicloud.core.modules.discord.util.snowflake

object RulePart : DiscordModulePart() {

    override val name: String = "Rule"
    override val commands: List<CommandBuilder> = listOf()
    lateinit var config: RuleConfig

    override suspend fun init() {
        config = if (!configManager.exists("discord:modules:rule")) {
            val config = RuleConfig()
            configManager.createConfig(config)
            config
        } else {
            configManager.getConfig("discord:modules:rule", RuleConfig::class.java)
        }
        loadConfigCommand()
    }

    private val channelCreate = kord.on<ChannelCreateEvent> {
        val guildChannel = channel.asChannelOf<GuildChannel>()
        if (guildChannel.guild != mainGuild) return@on
        val rulePermissions = mutableSetOf<Permission>(Permission.ViewChannel) to mutableSetOf<Permission>()
        val publicPermissions = mutableSetOf<Permission>() to mutableSetOf<Permission>(Permission.ViewChannel)
        val overwrites = mutableSetOf<Overwrite>()
        channel.data.permissionOverwrites.value?.let { list ->
            list.forEach { overwrite ->
                if (overwrite.id == config.acceptRole.snowflake) {
                    overwrite.allow.values.forEach {
                        if (!rulePermissions.first.contains(it) && !rulePermissions.second.contains(it)) {
                            rulePermissions.first.add(it)
                        }
                    }
                    overwrite.deny.values.forEach {
                        if (!rulePermissions.second.contains(it) && !rulePermissions.first.contains(it)) {
                            rulePermissions.second.add(it)
                        }
                    }
                } else if (overwrite.id == mainGuild.everyoneRole.id) {
                    overwrite.allow.values.forEach {
                        if (!publicPermissions.first.contains(it) && !publicPermissions.second.contains(it)) {
                            publicPermissions.first.add(it)
                        }
                    }
                    overwrite.deny.values.forEach {
                        if (!publicPermissions.second.contains(it) && !publicPermissions.first.contains(it)) {
                            publicPermissions.second.add(it)
                        }
                    }
                } else {
                    overwrites.add(overwrite)
                }
            }
        }
        val ruleOverwrite = Overwrite(
            config.acceptRole.snowflake,
            OverwriteType.Role,
            allow = Permissions(rulePermissions.first),
            deny = Permissions(rulePermissions.second)
        )
        val publicOverwrite = Overwrite(
            mainGuild.everyoneRole.id,
            OverwriteType.Role,
            allow = Permissions(publicPermissions.first),
            deny = Permissions(publicPermissions.second)
        )
        if (channel is TextChannel) {
            mainGuild.getChannelOf<TextChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
                permissionOverwrites!! += overwrites
            }
        } else if (channel is VoiceChannel) {
            mainGuild.getChannelOf<VoiceChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
                permissionOverwrites!! += overwrites
            }
        } else if (channel is NewsChannel) {
            mainGuild.getChannelOf<NewsChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
                permissionOverwrites!! += overwrites
            }
        } else if (channel is StageChannel) {
            mainGuild.getChannelOf<StageChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
                permissionOverwrites!! += overwrites
            }
        }
    }

    private val ruleMessageInteraction = kord.on<GuildButtonInteractionCreateEvent> {
        if (interaction.componentId != "rule_trigger") return@on
        val response = interaction.deferEphemeralResponse()

        val member = interaction.user.asMember()
        if (member.roleIds.contains(config.acceptRole.snowflake)) {
            member.removeRole(config.acceptRole.snowflake)
            response.respond {
                embed {
                    title = "Rules | DustreanNET"
                    description = "Your player role was revoked!"
                    useDefaultDesign(interaction.user)
                }
            }
            return@on
        }

        var existsRole = false
        member.getGuild().roleIds.forEach { if (it == config.acceptRole.snowflake) existsRole = true }

        if (!existsRole) {
            response.respond {
                embed {
                    title = "Error | DustreanNET"
                    description = "The role does not exist! Please contact an administrator!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(interaction.user)
                }
            }
            return@on
        }

        member.addRole(config.acceptRole.snowflake)
        response.respond {
            embed {
                title = "Rules | DustreanNET"
                description = "The player role was added to you!"
                useDefaultDesign(interaction.user)
            }
        }
    }

    private fun loadConfigCommand() {
        DiscordModuleMain.INSTANCE.configCommands.forEach {
            it.value.apply {
                group("rules", "Configure the rule module") {
                    subCommand("emoji", "Set the emoji for the accept button") {
                        string("emoji", "The emoji id for the accept button") {
                            required = true
                        }
                        boolean("force", "Force the bot to use the emoji if it's marked by the bot as invalid") {
                            required = false
                        }
                        perform(this@group, this@subCommand) {
                            val emojiMention = interaction.command.strings["emoji"]
                            var id: Long? = null
                            var name: String? = null
                            var animated = false
                            val force = interaction.command.booleans["force"] ?: false
                            if (emojiMention != null) {
                                if (emojiMention.startsWith("<a:")) {
                                    animated = true
                                    name = emojiMention.substring(3, emojiMention.length - 1).split(":")[0]
                                    id = emojiMention.substring(3, emojiMention.length - 1).split(":")[1].toLong()
                                } else if (emojiMention.startsWith("<:")) {
                                    name = emojiMention.substring(2, emojiMention.length - 1).split(":")[0]
                                    id = emojiMention.substring(2, emojiMention.length - 1).split(":")[1].toLong()
                                } else if (emojiMention.startsWith(":") && emojiMention.endsWith(":")) {
                                    name = emojiMention.substring(1, emojiMention.length - 1)
                                } else if (!force) {
                                    ioScope.launch {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | DustreanNET"
                                                description = "The emoji is invalid! (`$emojiMention`)"
                                                color = Color(250, 0, 0)
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                    }
                                    return@perform
                                } else {
                                    name = emojiMention
                                }
                            }
                            val emoji =
                                DiscordPartialEmoji(id?.snowflake, name, OptionalBoolean.Value(animated ?: false))
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                config.acceptEmoji = emoji {
                                    id = emoji.id?.value?.toLong()
                                    name = emoji.name
                                    animated = emoji.animated.asOptional.value ?: false
                                }
                                dev.redicloud.core.modules.discord.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The accept emoji was set to $emojiMention"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("role", "Set the role for the accept button") {
                        role("role", "The role for the accept button") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            val role = interaction.command.roles["role"]!!
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                config.acceptRole = role.id.value.toLong()
                                dev.redicloud.core.modules.discord.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The accept role was set to ${role.mention}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("create", "Create the rule message") {
                        channel("channel", "The channel where the message should be created") {
                            required = false
                        }
                        perform(this@group, this@subCommand) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val channelBehavior = interaction.command.channels["channel"] ?: interaction.channel
                                channelBehavior.asChannelOf<GuildMessageChannel>()
                                    .createMessage(config.ruleMessage, interaction.user, mutableMapOf()) {
                                        {
                                            button(ButtonStyle.Success, "rule_trigger") {
                                                emoji = config.acceptEmoji.partialEmoji()
                                            }
                                        }
                                    }.also {
                                        dev.redicloud.core.modules.discord.ioScope.launch {
                                            this@perform.interaction.respondEphemeral {
                                                embed {
                                                    title = "Info | DustreanNET"
                                                    description = "Rule message created in ${channelBehavior.mention}"
                                                    useDefaultDesign(this@perform.interaction.user)
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    subCommand("rules", "The rule message") {
                        message("message", "The rule message") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            val message = interaction.command.messages["message"]
                            if (message == null) {
                                dev.redicloud.core.modules.discord.ioScope.launch {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Error | DustreanNET"
                                            description = "The message is invalid! Please check the json format!"
                                            color = Color(250, 0, 0)
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                }
                                return@perform
                            }
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                config.ruleMessage = message
                                dev.redicloud.core.modules.discord.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The rule message was set!"
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