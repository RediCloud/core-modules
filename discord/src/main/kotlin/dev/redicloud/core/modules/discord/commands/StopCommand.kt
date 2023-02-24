package dev.redicloud.core.modules.discord.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.launch
import dev.redicloud.api.ICoreAPI
import dev.redicloud.core.modules.discord.DiscordModuleMain
import dev.redicloud.core.modules.discord.logChannel
import dev.redicloud.core.modules.discord.part.parts
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign

class StopCommand() : AbstractInputCommand("stop", null, "Stop the bot") {

    init {
        permissions = Permissions(Permission.All)
        perform {
            dev.redicloud.core.modules.discord.ioScope.launch {
                interaction.respondEphemeral {
                    embed {
                        title = "Stopping | DustreanNET"
                        description = "Stopping the bot..."
                        useDefaultDesign(interaction.user)
                    }
                }
                notifyStop(interaction.user)
                DiscordModuleMain.INSTANCE.onDisable(ICoreAPI.INSTANCE)
            }
        }
    }

}

val started = System.currentTimeMillis()
var stopped = false

suspend fun notifyStart() {
    logChannel.createMessage {
        embed {
            title = "Status | DustreanNET"
            description = ":green_square: State: Started\n" +
                    ":electric_plug: Version: Unknown\n" +
                    ":calendar: Date: ${
                        java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                        )
                    }\n" +
                    ":chains: Enabled parts: ${
                        parts.map { it.name }.joinToString(", ")
                    }\n" +
                    ":globe_with_meridians: Hostname: ${
                        if (System.getenv().containsKey("CORE_HOSTNAME")) {
                            System.getenv("CORE_HOSTNAME")
                        } else {
                            "Unknown"
                        }
                    }"
        }
    }
}

suspend fun notifyStop(user: User? = null) {
    if (stopped) return
    stopped = true
    logChannel.createMessage {
        embed {
            title = "Status | DustreanNET"
            description = ":red_square: State: Stopping\n" +
                    ":calendar: Date: ${
                        java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                        )
                    }\n" +
                    ":clock1: Uptime: ${
                        java.time.Duration.ofMillis(System.currentTimeMillis() - started).toHours()
                    } minutes\n" +
                    ":globe_with_meridians: Hostname: ${
                        if (System.getenv().containsKey("CORE_HOSTNAME")) {
                            System.getenv("CORE_HOSTNAME")
                        } else {
                            "Unknown"
                        }
                    }"
        }
    }
}