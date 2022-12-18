package net.dustrean.modules.discord.util

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import net.dustrean.modules.discord.kord

@Annotations.TopLevel.CommandDsl
interface CommandBuilder {
    val name: String
    val guildID: Snowflake
    var permissions: Permissions

    suspend fun create()
}

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 */
@Annotations.TopLevel.CommandDsl
class UserCommandBuilder(override val name: String, override val guildID: Snowflake) : CommandBuilder {

    override var permissions = Permissions()

    @Annotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildUserCommandInteractionCreateEvent.() -> Unit) =
        kord.on<GuildUserCommandInteractionCreateEvent> { event() }

    override suspend fun create() {
        kord.createGuildUserCommand(guildID, name) {
            defaultMemberPermissions = permissions
        }
    }

}

@Annotations.TopLevel.CommandDsl
suspend inline fun userCommand(
    name: String, guildID: Snowflake, crossinline builder: UserCommandBuilder.() -> Unit
) = UserCommandBuilder(name, guildID).apply(builder).also { it.create() }

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 */
@Annotations.TopLevel.CommandDsl
class MessageCommandBuilder(override val name: String, override val guildID: Snowflake) : CommandBuilder {

    override var permissions = Permissions()

    @Annotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildMessageCommandInteractionCreateEvent.() -> Unit) =
        kord.on<GuildMessageCommandInteractionCreateEvent> { event() }

    override suspend fun create() {
        kord.createGuildMessageCommand(guildID, name) {
            defaultMemberPermissions = permissions
        }
    }

}

@Annotations.TopLevel.CommandDsl
suspend inline fun messageCommand(
    name: String, guildID: Snowflake, crossinline builder: MessageCommandBuilder.() -> Unit
) = MessageCommandBuilder(name, guildID).apply(builder).also { it.create() }


/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 * @param description The description which is displayed when tabbing
 */
@Annotations.TopLevel.CommandDsl
class InputCommandBuilder(
    override val name: String, override val guildID: Snowflake, private val description: String
) : CommandBuilder {

    override var permissions = Permissions()
    var chatInputBuilder: suspend ChatInputCreateBuilder.() -> Unit = { }
    var subCommands = mutableListOf<Triple<String, String, suspend SubCommandBuilder.() -> Unit>>()
    var groups = mutableListOf<Triple<String, String, suspend GroupCommandBuilder.() -> Unit>>()

    @Annotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildChatInputCommandInteractionCreateEvent.() -> Unit) =
        kord.on<GuildChatInputCommandInteractionCreateEvent> { event() }

    @Annotations.BuildLevel.ConfigDsl
    fun subCommand(name: String, description: String, block: suspend SubCommandBuilder.() -> Unit) {
        subCommands += Triple(name, description, block)
    }

    @Annotations.BuildLevel.ConfigDsl
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

@Annotations.TopLevel.CommandDsl
suspend inline fun inputCommand(
    name: String, guildID: Snowflake, description: String, crossinline builder: InputCommandBuilder.() -> Unit
) = InputCommandBuilder(name, guildID, description).apply(builder).also { it.create() }

private class Annotations {
    class TopLevel {
        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
        @DslMarker
        annotation class CommandDsl
    }

    class BuildLevel {
        @DslMarker
        annotation class RunsDsl

        @DslMarker
        annotation class ConfigDsl
    }
}