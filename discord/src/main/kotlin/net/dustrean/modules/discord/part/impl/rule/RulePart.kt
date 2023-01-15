package net.dustrean.modules.discord.part.impl.rule

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.*
import dev.kord.core.event.channel.ChannelCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import net.dustrean.modules.discord.configManager
import net.dustrean.modules.discord.kord
import net.dustrean.modules.discord.mainGuild
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder
import net.dustrean.modules.discord.util.interactions.InteractionCommandID
import net.dustrean.modules.discord.util.interactions.button
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
        emoji = DiscordPartialEmoji(name = config.acceptEmoji, id = null)

        perform {
            if (interaction.guildId != mainGuild.id) return@perform
            val response = interaction.deferEphemeralResponse()

            val member = interaction.user.asMember()
            if (member.roleIds.contains(config.acceptRole.snowflake)) {
                member.removeRole(config.acceptRole.snowflake)
                response.respond {
                    content = "Your player role was revoked!"
                }
                return@perform
            }

            member.addRole(config.acceptRole.snowflake)
            response.respond {
                content = "The player role was added to you!"
            }
        }
    }

}