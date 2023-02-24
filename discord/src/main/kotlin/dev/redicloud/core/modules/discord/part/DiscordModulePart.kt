package dev.redicloud.core.modules.discord.part

import dev.redicloud.core.modules.discord.part.impl.moderation.ModerationPart
import dev.redicloud.core.modules.discord.part.impl.rule.RulePart
import dev.redicloud.core.modules.discord.part.impl.ticket.TicketPart
import dev.redicloud.core.modules.discord.util.commands.CommandBuilder

val parts = listOf(RulePart, ModerationPart, TicketPart)

fun isLoaded(clazz: Class<out DiscordModulePart>): Boolean {
    return parts.any { it::class.java == clazz }
}

abstract class DiscordModulePart {

    abstract val name: String

    abstract suspend fun init()

    abstract val commands: List<CommandBuilder>
}