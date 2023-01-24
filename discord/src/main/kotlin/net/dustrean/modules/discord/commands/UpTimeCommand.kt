package net.dustrean.modules.discord.commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.launch
import net.dustrean.modules.discord.ioScope
import net.dustrean.modules.discord.util.message.useDefaultDesign
import java.time.Duration

class UpTimeCommand : AbstractInputCommand("uptime", null, "Information about the uptime of the bot") {

    init {
        perform {
            ioScope.launch {
                interaction.respondEphemeral {
                    embed {
                        title = "Uptime | DustreanNET"
                        description = "The bot has been running for ${
                            Duration.ofMillis((System.currentTimeMillis() - started)).toHours()
                        }"
                        useDefaultDesign(interaction.user)
                    }
                }
            }
        }
    }

}