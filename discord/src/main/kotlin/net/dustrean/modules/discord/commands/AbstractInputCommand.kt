package net.dustrean.modules.discord.commands

import dev.kord.common.entity.Snowflake
import net.dustrean.modules.discord.util.commands.InputCommandBuilder

val COMMANDS = mutableListOf<AbstractInputCommand>()

abstract class AbstractInputCommand(override val name: String, override val guildID: Snowflake?, val description: String) : InputCommandBuilder(name, guildID, description)