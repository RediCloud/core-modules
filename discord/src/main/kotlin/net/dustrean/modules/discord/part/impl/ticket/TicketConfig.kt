package net.dustrean.modules.discord.part.impl.ticket

import dev.kord.core.entity.User
import net.dustrean.modules.discord.data.AbstractDiscordConfig
import kotlin.time.Duration.Companion.days

class TicketConfig() : AbstractDiscordConfig() {

    override val key = "discord:module:ticket"
    var openMessages = mutableListOf<Long>()
    var category = 1064653389063012532L
    var archiveChannel = 1064653495925473371L
    var supportRole = 1064654488507514983L
    var publicSupportChannels = mutableListOf(1064654802258247700L)
    var channelPrefix = "ticket-"
    var channelIdentifierType = TicketIdentifierType.NUMBER
    var count = 0
    var tagAfterNoResponse = 3.days.inWholeMilliseconds
    var closeAfterNoResponse = 7.days.inWholeMilliseconds
    var openEmoji: Emoji = Emoji(null, "✅")
    var maxOpenTicketsPerUser = 1
    var ticketWelcomeMessage = Embed(
        title = "Welcome to your ticket!",
        description = "Hi {user},\nwelcome to your ticket!",
        fields = mutableListOf<EmbedField>().apply {
            add(EmbedField("No interaction", "Your ticket will be archived after 7 days of inactivity. You will be notified after 3 days of inactivity."))
        }
    )

}

enum class TicketIdentifierType {
    USER_ID,
    NUMBER,
    USER_NAME;

    fun parse(user: User): String {
        return when(this) {
            USER_ID -> user.id.value.toString()
            NUMBER -> TicketPart.config.count++.toString()
            USER_NAME -> user.username
        }
    }
}