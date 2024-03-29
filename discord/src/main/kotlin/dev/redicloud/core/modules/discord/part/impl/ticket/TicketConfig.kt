package dev.redicloud.core.modules.discord.part.impl.ticket

import dev.kord.core.entity.User
import dev.redicloud.api.ICoreAPI
import dev.redicloud.api.utils.networkName
import dev.redicloud.core.modules.discord.data.AbstractDiscordConfig
import dev.redicloud.core.modules.discord.data.chat.embed
import dev.redicloud.core.modules.discord.data.chat.Emoji
import dev.redicloud.core.modules.discord.data.chat.message
import dev.redicloud.core.modules.discord.ioScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class TicketConfig() : AbstractDiscordConfig() {

    override val key = "discord:modules:ticket"
    var openMessages = mutableListOf<Long>()
    var category = 1064653389063012532L
    var archiveCategory = 1066077906897490041L
    var archiveChannel = 1064653495925473371L
    var deleteAfterArchive = 21.days.inWholeMilliseconds
    var archiveViewRole = 1065726124056903681L
    var supportRole = 1064654488507514983L
    var publicSupportChannels = mutableListOf(1064654802258247700L) //TODO
    var channelPrefix = "ticket-"
    var channelIdentifierType = TicketIdentifierType.NUMBER
    var count = 0
    var tagAfterNoResponse = 3.days.inWholeMilliseconds
    var closeAfterNoResponse = 7.days.inWholeMilliseconds
    var openEmoji = Emoji(name = "✅")
    var closeEmoji = Emoji(name = "❌")
    var confirmEmoji = Emoji(name = "✅")
    var maxOpenTicketsPerUser = 1
    var inactivityNotifyMessage = message {
        embed {
            title = "Inactivity | $networkName"
            description = "Your ticket has been inactive for ${tagAfterNoResponse.milliseconds.inWholeDays} days. If you still need help, please write a message to not get your ticket closed automatically in ${closeAfterNoResponse.milliseconds.inWholeDays - tagAfterNoResponse.milliseconds.inWholeDays} days."
            color = intArrayOf(250, 0, 0)
        }
    }
    var inactivityCloseMessage = message {
        embed {
            title = "Inactivity | $networkName"
            description = "Your ticket has been inactive for ${closeAfterNoResponse.milliseconds.inWholeDays} days and has been closed automatically!"
            color = intArrayOf(250, 0, 0)
        }
    }
    var closeConfirmMessage = message {
        embed {
            title = "Confirm | $networkName"
            description = "Are you sure you want to close this ticket?"
            color = intArrayOf(250, 0, 0)
        }
    }
    var ticketWelcomeMessage = message {
        embed {
            title = "Welcome | $networkName"
            description = "Welcome {user},\n" +
                    "we are glad that you have reached out to us :wave:\n" +
                    "\n" +
                    ":clock1: **Inactivity**\n" +
                    "Please note that tickets will be closed after 7 days of inactivity, but you will receive a reminder after 3 days of inactivity.\n" +
                    "\n" +
                    ":point_right: **Interactions**\n" +
                    "- close the ticket with `/ticket close` or use the {close_emoji} reaction\n" +
                    "- add a user to this ticket with `/ticket add @user`\n" +
                    "\n" +
                    ":warning: Important\n" +
                    "Please do not ping any team members! They will respond as soon as they can.\n" +
                    "\n" +
                    "\n" +
                    "When you understand this, please react with the {confirm_emoji} emoji to confirm that you have read this message."
        }
    }
    var confirmMessage = message {
        embed {
            title = "Confirmed | $networkName"
            description = "Your ticket has been confirmed! Please explain your issue in detail! A staff member will contact you as soon as possible!"
            defaultDesign = true
        }
    }

}

enum class TicketIdentifierType {
    USER_ID,
    NUMBER,
    USER_NAME;

    fun parse(user: User): String {
        return when(this) {
            USER_ID -> user.id.value.toString()
            NUMBER -> {
                val count = TicketPart.config.count
                ioScope.launch {
                    TicketPart.config.count++
                    ICoreAPI.INSTANCE.configManager.saveConfig(TicketPart.config)
                }
                count.toString()
            }
            USER_NAME -> user.username
        }
    }
}