package net.dustrean.modules.discord.part

import net.dustrean.modules.discord.util.CommandBuilder

val parts = listOf<DiscordModulePart>()

abstract class DiscordModulePart {
    abstract val name: String

    abstract suspend fun init()

    abstract val commands: List<CommandBuilder>
}