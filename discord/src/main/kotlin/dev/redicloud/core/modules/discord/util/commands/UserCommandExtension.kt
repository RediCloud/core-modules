package dev.redicloud.core.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.collect
import dev.redicloud.core.modules.discord.kord

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 */
@CommandAnnotations.TopLevel.CommandDsl
class UserCommandBuilder(override val name: String, override val guildID: Snowflake?) : CommandBuilder {

    override var permissions = Permissions()

    @CommandAnnotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildUserCommandInteractionCreateEvent.() -> Unit) =
        dev.redicloud.core.modules.discord.kord.on<GuildUserCommandInteractionCreateEvent> {
            if (interaction.invokedCommandName != name) return@on
            event()
        }

    override suspend fun create() {
        if (guildID != null) {
            create(guildID)
            return
        }
        dev.redicloud.core.modules.discord.kord.guilds.collect() {
            create(it.id)
        }
    }

    private suspend fun create(guildID: Snowflake) {
        dev.redicloud.core.modules.discord.kord.createGuildUserCommand(guildID, name) {
            defaultMemberPermissions = permissions
        }
    }

}

@CommandAnnotations.TopLevel.CommandDsl
inline fun userCommand(
    name: String, guildID: Snowflake, crossinline builder: UserCommandBuilder.() -> Unit
) = UserCommandBuilder(name, guildID).apply(builder)