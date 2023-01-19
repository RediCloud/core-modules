package net.dustrean.modules.discord.part.impl.ticket

import dev.kord.core.entity.User
import kotlinx.coroutines.launch
import net.dustrean.api.ICoreAPI
import net.dustrean.modules.discord.data.AbstractDiscordConfig
import net.dustrean.modules.discord.data.Emoji
import net.dustrean.modules.discord.data.embed
import net.dustrean.modules.discord.data.messages
import net.dustrean.modules.discord.ioScope
import kotlin.time.Duration.Companion.days

class TicketConfig() : AbstractDiscordConfig() {

    override val key = "discord:modules:ticket"
    var openMessages = mutableListOf<Long>()
    var category = 1064653389063012532L
    var archiveCategory = 1064653495925473371L
    var deleteAfterArchive = 21.days.inWholeMilliseconds
    var archiveViewRole = 1065726124056903681
    var supportRole = 1064654488507514983L
    var publicSupportChannels = mutableListOf(1064654802258247700L) //TODO
    var channelPrefix = "ticket-"
    var channelIdentifierType = TicketIdentifierType.NUMBER
    var count = 0
    var tagAfterNoResponse = 3.days.inWholeMilliseconds
    var closeAfterNoResponse = 7.days.inWholeMilliseconds
    var openEmoji = Emoji(null, "✅")
    var closeEmoji = Emoji(null, "❌")
    var confirmEmoji = Emoji(null, "✅")
    var maxOpenTicketsPerUser = 1
    var inactivityNotifyMessages = messages {
        embed {
            title = "Inactivity | DustreanNET"
            description = "Your ticket has been inactive for ${tagAfterNoResponse.days} days. If you still need help, please write a message to not get your ticket closed automatically in ${closeAfterNoResponse.days - tagAfterNoResponse.days} days."
            color = intArrayOf(250, 0, 0)
        }
    }
    var inactivityCloseMessages = messages {
        embed {
            title = "Inactivity | DustreanNET"
            description = "Your ticket has been inactive for ${closeAfterNoResponse.days} days and has been closed automatically!"
            color = intArrayOf(250, 0, 0)
        }
    }
    var closeConfirmMessages = messages {
        embed {
            title = "Confirm | DustreanNET"
            description = "Are you sure you want to close this ticket?"
            color = intArrayOf(250, 0, 0)
        }
    }
    var ticketWelcomeMessages = messages {
        embed {
            title = "Welcome | DustreanNET"
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
    var confirmMessage = messages {
        embed {
            title = "Confirmed | DustreanNET"
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
                    ICoreAPI.INSTANCE.getConfigManager().saveConfig(TicketPart.config)
                }
                count.toString()
            }
            USER_NAME -> user.username
        }
    }
}