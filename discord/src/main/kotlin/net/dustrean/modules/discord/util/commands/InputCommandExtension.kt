package net.dustrean.modules.discord.util.commands

import dev.kord.common.annotation.KordDsl
import dev.kord.common.entity.ApplicationCommandOptionType
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import kotlinx.coroutines.Job
import net.dustrean.modules.discord.kord
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val listener = kord.on<GuildChatInputCommandInteractionCreateEvent> {
    commands.forEach {
        if (interaction.command.rootName != it.name) return@forEach
        val data = interaction.command.data
        val options = data.options.value
        options?.forEach{ optionData ->
            val groupName = optionData.name
            if (optionData.subCommands.value == null || optionData.subCommands.value!!.isEmpty()) {
                val perform = it.actions[groupName] ?: return@on //group name is here the subcommand name
                perform(this)
                return@on
            }
            val subCommandName = optionData.subCommands.value!![0].name
            val perform = it.actions["${groupName}_${subCommandName}"] ?: return@on
            perform(this)
            return@on
        }
    }
}
private val commands = mutableListOf<InputCommandBuilder>()

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 * @param description The description which is displayed when tabbing
 */
@CommandAnnotations.TopLevel.CommandDsl
class InputCommandBuilder(
    override val name: String, override val guildID: Snowflake, private val description: String
) : CommandBuilder {

    init {
        commands += this
    }

    override var permissions = Permissions()
    var chatInputBuilder: suspend ChatInputCreateBuilder.() -> Unit = { }
    var subCommands =
        mutableListOf<Triple<String, String, suspend SubCommandBuilder.() -> Unit>>()
    var groups =
        mutableListOf<Triple<String, String, suspend GroupCommandBuilder.() -> Unit>>()
    var actions =
        mutableMapOf<String, ((GuildChatInputCommandInteractionCreateEvent) -> Unit)?>()

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

@kotlin.contracts.ExperimentalContracts
fun BaseInputChatBuilder.embed(name: String, description: String, required: Boolean = true) {
    string(name, description) {
        this.required = required
    }
}