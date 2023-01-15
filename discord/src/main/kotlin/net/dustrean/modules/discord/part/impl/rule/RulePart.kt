package net.dustrean.modules.discord.part.impl.rule

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import net.dustrean.modules.discord.config
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

    override suspend fun init() {
        ruleMessageHandler
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
        emoji = DiscordPartialEmoji(name = "✅", id = null)

        perform {
            if (interaction.guildId != mainGuild.id) return@perform
            val response = interaction.deferEphemeralResponse()

            val member = interaction.user.asMember()
            if (member.roleIds.contains(config.roles.playerID.snowflake)) {
                member.removeRole(config.roles.playerID.snowflake)
                response.respond {
                    content = "Your player role was revoked!"
                }
                return@perform
            }

            member.addRole(config.roles.playerID.snowflake)
            response.respond {
                content = "The player role was added to you!"
            }
        }
    }

}