package net.dustrean.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import kotlinx.coroutines.Job
import net.dustrean.modules.discord.kord

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 * @param description The description which is displayed when tabbing
 */
@CommandAnnotations.TopLevel.CommandDsl
class InputCommandBuilder(
    override val name: String, override val guildID: Snowflake, private val description: String
) : CommandBuilder {

    override var permissions = Permissions()
    var chatInputBuilder: suspend ChatInputCreateBuilder.() -> Unit = { }
    var subCommands =
        mutableListOf<Triple<String, String, suspend SubCommandBuilder.() -> Unit>>()
    var groups =
        mutableListOf<Triple<String, String, suspend GroupCommandBuilder.() -> Unit>>()
    var actions =
        mutableMapOf<String, ((GuildChatInputCommandInteractionCreateEvent) -> Unit)?>()
    private val listener = kord.on<GuildChatInputCommandInteractionCreateEvent> {
        if (interaction.command.rootName != name) return@on
        val data = interaction.command.data
        val options = data.options.value
        options?.forEach { optionData ->
            val groupName = optionData.name
            println("Group: $groupName")
            if (optionData.subCommands.value == null || optionData.subCommands.value!!.isEmpty()) return@on
            val subCommandName = optionData.subCommands.value!![0].name
            println("SubCommand: $subCommandName")
            val perform = actions["${groupName}_${subCommandName}"] ?: return@on
            println("Performing subcommand")
            perform(this)
            return@forEach
        }
    }

    @CommandAnnotations.BuildLevel.RunsDsl
    fun perform(groupContext: GroupCommandBuilder? = null, context: SubCommandBuilder, event: GuildChatInputCommandInteractionCreateEvent.() -> Unit) {
        var key = context.name
        if (groupContext != null) key = "${groupContext.name}_$key"
        actions[key] = event
    }

    @CommandAnnotations.BuildLevel.ConfigDsl
    fun subCommand(name: String, description: String, block: suspend SubCommandBuilder.() -> Unit) {
        subCommands += Triple(name, description, block)
    }

    @CommandAnnotations.BuildLevel.ConfigDsl
    fun group(name: String, description: String, block: suspend GroupCommandBuilder.() -> Unit) {
        groups += Triple(name, description, block)
    }

    override suspend fun create() {
        kord.createGuildChatInputCommand(guildID, name, description) {
            defaultMemberPermissions = permissions
            subCommands.forEach { (name, description, builder) ->
                this@createGuildChatInputCommand.subCommand(name, description) { builder() }
            }
            groups.forEach { (name, description, builder) ->
                this@createGuildChatInputCommand.group(name, description) { builder() }
            }
            chatInputBuilder()
        }
    }

}

@CommandAnnotations.TopLevel.CommandDsl
inline fun inputCommand(
    name: String, guildID: Snowflake, description: String, crossinline builder: InputCommandBuilder.() -> Unit
) = InputCommandBuilder(name, guildID, description).apply(builder)