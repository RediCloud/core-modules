package net.dustrean.modules.discord.part

import net.dustrean.modules.discord.part.impl.moderation.ModerationPart
import net.dustrean.modules.discord.part.impl.rule.RulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder

val parts = listOf(RulePart, ModerationPart)

abstract class DiscordModulePart {

    abstract val name: String

    abstract suspend fun init()

    abstract val commands: List<CommandBuilder>
}