package net.dustrean.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.on
import net.dustrean.modules.discord.kord

/**
 * @param name The name which is displayed when performing the command
 * @param guildID The guild where the command should be work
 */
@CommandAnnotations.TopLevel.CommandDsl
class MessageCommandBuilder(override val name: String, override val guildID: Snowflake) : CommandBuilder {

    override var permissions = Permissions()

    @CommandAnnotations.BuildLevel.RunsDsl
    inline fun perform(crossinline event: GuildMessageCommandInteractionCreateEvent.() -> Unit) =
        kord.on<GuildMessageCommandInteractionCreateEvent> {
            if (interaction.invokedCommandName != name) return@on
            event()
        }

    override suspend fun create() {
        kord.createGuildMessageCommand(guildID, name) {
            defaultMemberPermissions = permissions
        }
    }

}

@CommandAnnotations.TopLevel.CommandDsl
suspend inline fun messageCommand(
    name: String, guildID: Snowflake, crossinline builder: MessageCommandBuilder.() -> Unit
) = MessageCommandBuilder(name, guildID).apply(builder).also { it.create() }