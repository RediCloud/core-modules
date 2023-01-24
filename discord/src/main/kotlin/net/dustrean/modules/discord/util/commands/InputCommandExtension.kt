package net.dustrean.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import net.dustrean.modules.discord.data.chat.Message
import net.dustrean.modules.discord.data.chat.toMessage
import net.dustrean.modules.discord.kord

private val listener = kord.on<GuildChatInputCommandInteractionCreateEvent> {
    commands.forEach {
        if (interaction.command.rootName != it.name) return@forEach
        val data = interaction.command.data
        val options = data.options.value
        if (options.isNullOrEmpty()) {
            val perform = it.actions["_default"] ?: return@on
            perform(this)
            return@on
        }
        options.forEach{ optionData ->
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
open class InputCommandBuilder(
    override val name: String, override val guildID: Snowflake?, private val description: String
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
    fun perform(groupContext: GroupCommandBuilder? = null, context: SubCommandBuilder? = null, event: GuildChatInputCommandInteractionCreateEvent.() -> Unit) {
        if (groupContext == null && context == null) {
            actions["_default"] = event
            return
        }
        var key = context!!.name
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
        if (guildID != null) {
            create(guildID!!)
            return
        }
        kord.guilds.collect() {
            create(it.id)
        }
    }

    private suspend fun create(guildID: Snowflake) {
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

fun BaseInputChatBuilder.message(name: String, description: String, builder: StringChoiceBuilder.() -> Unit = {}) {
    string(name, description, builder)
}

val InteractionCommand.messages: Map<String, Message> get() {
    val map = mutableMapOf<String, Message>()
    strings.forEach { (name, value) ->
        if (!value.startsWith("{") || !value.endsWith("}")) return@forEach
        try {
            val message = toMessage(value)
            map[name] = message
        }catch (e: Exception) {
            e.printStackTrace()
            return@forEach
        }
    }
    return map
}