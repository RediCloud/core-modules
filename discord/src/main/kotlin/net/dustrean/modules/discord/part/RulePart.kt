package net.dustrean.modules.discord.part

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import net.dustrean.modules.discord.config
import net.dustrean.modules.discord.kord
import net.dustrean.modules.discord.mainGuild
import net.dustrean.modules.discord.util.CommandBuilder
import net.dustrean.modules.discord.util.InteractionCommandIDs
import net.dustrean.modules.discord.util.snowflake

object RulePart : DiscordModulePart() {

    override val name: String = "Rule"
    override val commands: List<CommandBuilder> = listOf()

    override suspend fun init() {
        ruleTriggerEvent
        ruleMessageCreate
    }

    private val ruleTriggerEvent = kord.on<GuildButtonInteractionCreateEvent> {
        if (interaction.guildId != mainGuild.id) return@on
        if (interaction.component.customId != InteractionCommandIDs.RULE_TRIGGER) return@on
        val response = interaction.deferEphemeralResponse()
        val member = interaction.user.asMember()
        if (member.roleIds.contains(config.roles.playerID.snowflake)) {
            member.removeRole(config.roles.playerID.snowflake)
            response.respond {
                content = "Your player role was revoked!"
            }
            return@on
        }

        member.addRole(config.roles.playerID.snowflake)
        response.respond {
            content = "The player role was added to you!"
        }
    }

    private val ruleMessageCreate = kord.on<MessageCreateEvent> {
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

            actionRow {
                interactionButton(ButtonStyle.Success, InteractionCommandIDs.RULE_TRIGGER) {
                    emoji(ReactionEmoji.Unicode("✅"))
                }
            }
        }
    }

}