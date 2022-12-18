package net.dustrean.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent
import dev.kord.core.on
import net.dustrean.modules.discord.kord

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 */
@CommandAnnotations.TopLevel.CommandDsl
class UserCommandBuilder(override val name: String, override val guildID: Snowflake) : CommandBuilder {

    override var permissions = Permissions()

    @CommandAnnotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildUserCommandInteractionCreateEvent.() -> Unit) =
        kord.on<GuildUserCommandInteractionCreateEvent> { event() }

    override suspend fun create() {
        kord.createGuildUserCommand(guildID, name) {
            defaultMemberPermissions = permissions
        }
    }

}

@CommandAnnotations.TopLevel.CommandDsl
suspend inline fun userCommand(
    name: String, guildID: Snowflake, crossinline builder: UserCommandBuilder.() -> Unit
) = UserCommandBuilder(name, guildID).apply(builder).also { it.create() }