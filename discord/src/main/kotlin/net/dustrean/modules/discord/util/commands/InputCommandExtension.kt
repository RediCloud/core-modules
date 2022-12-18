package net.dustrean.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
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
    var subCommands = mutableListOf<Triple<String, String, suspend SubCommandBuilder.() -> Unit>>()
    var groups = mutableListOf<Triple<String, String, suspend GroupCommandBuilder.() -> Unit>>()

    @CommandAnnotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildChatInputCommandInteractionCreateEvent.() -> Unit) =
        kord.on<GuildChatInputCommandInteractionCreateEvent> { event() }

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
                this.subCommand(name, description) { builder() }
            }
            groups.forEach { (name, description, builder) ->
                this.group(name, description) { builder() }
            }
            chatInputBuilder()
        }
    }

}

@CommandAnnotations.TopLevel.CommandDsl
suspend inline fun inputCommand(
    name: String, guildID: Snowflake, description: String, crossinline builder: InputCommandBuilder.() -> Unit
) = InputCommandBuilder(name, guildID, description).apply(builder).also { it.create() }