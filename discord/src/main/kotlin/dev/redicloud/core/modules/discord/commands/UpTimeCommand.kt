package dev.redicloud.core.modules.discord.commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.message.create.embed
import dev.redicloud.core.modules.discord.ioScope
import kotlinx.coroutines.launch
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign
import java.time.Duration

class UpTimeCommand : AbstractInputCommand("uptime", null, "Information about the uptime of the bot") {

    init {
        perform {
            ioScope.launch {
                interaction.respondEphemeral {
                    embed {
                        title = "Uptime | $networkName"
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