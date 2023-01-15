package net.dustrean.modules.discord.part.impl.rule

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.common.entity.optional.optional
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.*
import dev.kord.core.event.channel.ChannelCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.role
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.launch
import net.dustrean.modules.discord.*
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder
import net.dustrean.modules.discord.util.interactions.InteractionCommandID
import net.dustrean.modules.discord.util.interactions.button
import net.dustrean.modules.discord.util.message.useDefaultFooter
import net.dustrean.modules.discord.util.message.userFooter
import net.dustrean.modules.discord.util.snowflake

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
        val ruleOverwrite = Overwrite(
            config.acceptRole.snowflake,
            OverwriteType.Role,
            allow = Permissions(Permission.ViewChannel),
            deny = Permissions()
        )
        val publicOverwrite = Overwrite(
            mainGuild.everyoneRole.id,
            OverwriteType.Role,
            allow = Permissions(),
            deny = Permissions(Permission.ViewChannel)
        )
        if (channel is TextChannel) {
            mainGuild.getChannelOf<TextChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
            }
        } else if (channel is VoiceChannel) {
            mainGuild.getChannelOf<VoiceChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
            }
        }else if(channel is NewsChannel) {
            mainGuild.getChannelOf<NewsChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
            }
        }else if(channel is StageChannel) {
            mainGuild.getChannelOf<StageChannel>(channel.id).edit {
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                permissionOverwrites!! += ruleOverwrite
                permissionOverwrites!! += publicOverwrite
            }
        }
    }

    private val ruleMessageHandler = kord.on<MessageCreateEvent> {
        if (message.content != "!rules") return@on
        if (member == null) return@on
        if (!member!!.getPermissions().contains(Permission.ManageMessages)) return@on

        message.channel.createMessage {
            config.ruleMessages.forEach { rule ->
                embed {
                    color = Color(rule.color)
                    title = rule.title
                    description = rule.description
                }
            }

            actionRow { acceptButton() }
        }
    }

    private fun ActionRowBuilder.acceptButton() = button(ButtonStyle.Success, InteractionCommandID.RULE_TRIGGER) {
        emoji = DiscordPartialEmoji(config.acceptEmoji.id?.snowflake, config.acceptEmoji.name, OptionalBoolean.Value(config.acceptEmoji.animated))

        perform {
            if (interaction.guildId != mainGuild.id) return@perform
            val response = interaction.deferEphemeralResponse()

            val member = interaction.user.asMember()
            if (member.roleIds.contains(config.acceptRole.snowflake)) {
                member.removeRole(config.acceptRole.snowflake)
                response.respond {
                    embed {
                        title = "Rules | DustreanNET"
                        description = "Your player role was revoked!"
                        useDefaultFooter(interaction.user)
                    }
                }
                return@perform
            }

            member.addRole(config.acceptRole.snowflake)
            response.respond {
                embed {
                    title = "Rules | DustreanNET"
                    description = "The player role was added to you!"
                    useDefaultFooter(interaction.user)
                }
            }
        }
    }

    private fun loadConfigCommand(){
        DiscordModuleMain.CONFIG_COMMANDS.forEach {
            it.value.apply {
                group("rules", "Configure the rule module") {
                    subCommand("emoji", "Set the emoji for the accept button") {
                        string("emoji", "The emoji id for the accept button") {
                            required = false
                        }
                        perform(this@group, this@subCommand) {
                            val emojiMention = interaction.command.strings["emoji"]
                            var id: Long? = null
                            var name: String? = null
                            var animated = false
                            if (emojiMention != null) {
                                if (emojiMention.startsWith("<a:")) {
                                    animated = true
                                    name = emojiMention.substring(3, emojiMention.length - 1).split(":")[0]
                                    id = emojiMention.substring(3, emojiMention.length - 1).split(":")[1].toLong()
                                } else if (emojiMention.startsWith("<:")) {
                                    name = emojiMention.substring(2, emojiMention.length - 1).split(":")[0]
                                    id = emojiMention.substring(2, emojiMention.length - 1).split(":")[1].toLong()
                                } else if(emojiMention.startsWith(":") && emojiMention.endsWith(":")) {
                                    name = emojiMention.substring(1, emojiMention.length - 1)
                                } else {
                                    ioScope.launch {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | DustreanNET"
                                                description = "The emoji is invalid! ($emojiMention)"
                                                color = Color(250, 0, 0)
                                                useDefaultFooter(interaction.user)
                                            }
                                        }
                                    }
                                    return@perform
                                }
                            }
                            val emoji = DiscordPartialEmoji(id?.snowflake, name, OptionalBoolean.Value(animated ?: false))
                            val rawEmoji = if(emoji.id != null) "<${if(emoji.animated == OptionalBoolean.Value(true)) "a" else ""}:${emoji.name}:${emoji.id}>" else emoji.name
                            ioScope.launch {
                                config.acceptEmoji = Emoji(emoji.id?.value?.toLong(), emoji.name, emoji.animated.asOptional.value ?: false)
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The accept emoji was set to $rawEmoji"
                                        useDefaultFooter(interaction.user)
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
                            ioScope.launch {
                                config.acceptRole = role.id.value.toLong()
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
                                        description = "The accept role was set to ${role.mention}"
                                        useDefaultFooter(interaction.user)
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