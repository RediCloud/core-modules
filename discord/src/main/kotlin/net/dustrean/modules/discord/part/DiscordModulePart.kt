package net.dustrean.modules.discord.part

import net.dustrean.modules.discord.part.impl.RulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder

val parts = listOf<DiscordModulePart>(RulePart)

abstract class DiscordModulePart {
    abstract val name: String

    abstract suspend fun init()

    abstract val commands: List<CommandBuilder>
}