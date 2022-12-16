package net.dustrean.modules.discord.util

import dev.kord.common.entity.Permissions
import dev.kord.core.entity.Guild
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.*
import net.dustrean.modules.discord.kord

sealed interface CommandBuilder {
    val name: String
    val guild: Guild
    var permissions: Permissions

    suspend fun create()
}

/**
 * @param name The name which is displayed when performing the command
 */
sealed class UserCommandBuilder(override val name: String, override val guild: Guild) : CommandBuilder {

    override var permissions = Permissions()

    inline fun perform(crossinline event: GuildUserCommandInteractionCreateEvent.() -> Unit) {
        kord.on<GuildUserCommandInteractionCreateEvent> { event() }
    }

    override suspend fun create() {
        kord.createGuildUserCommand(guild.id, name) {
            defaultMemberPermissions = permissions
        }
    }

}

sealed class MessageCommandBuilder(override val name: String, override val guild: Guild) : CommandBuilder {

    override var permissions = Permissions()

    inline fun perform(crossinline event: GuildMessageCommandInteractionCreateEvent.() -> Unit) {
        kord.on<GuildMessageCommandInteractionCreateEvent> { event() }
    }

    override suspend fun create() {
        kord.createGuildMessageCommand(guild.id, name) {
            defaultMemberPermissions = permissions
        }
    }

}

sealed class InputCommandBuilder(
    override val name: String, override val guild: Guild, private val description: String
) : CommandBuilder {

    override var permissions = Permissions()
    var chatInputBuilder: suspend ChatInputCreateBuilder.() -> Unit = { }
    var subCommands = mutableListOf<Triple<String, String, suspend SubCommandBuilder.() -> Unit>>()
    var groups = mutableListOf<Triple<String, String, suspend GroupCommandBuilder.() -> Unit>>()

    inline fun perform(crossinline event: GuildChatInputCommandInteractionCreateEvent.() -> Unit) {
        kord.on<GuildChatInputCommandInteractionCreateEvent> { event() }
    }

    fun subCommand(name: String, description: String, block: suspend SubCommandBuilder.() -> Unit) {
        subCommands += Triple(name, description, block)
    }

    fun group(name: String, description: String, block: suspend GroupCommandBuilder.() -> Unit) {
        groups += Triple(name, description, block)
    }

    override suspend fun create() {
        kord.createGuildChatInputCommand(guild.id, name, description) {
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