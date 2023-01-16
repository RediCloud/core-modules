package net.dustrean.modules.discord.part.impl.ticket

import net.dustrean.api.config.IConfig
import kotlin.time.Duration.Companion.days

class TicketConfig() : IConfig {

    override val key = "discord:module:ticket"
    var ticketCategory = 1064653389063012532L
    var archiveChannel = 1064653495925473371L
    var ticketSupportRole = 1064654488507514983L
    var publicSupportChannels = listOf(1064654802258247700L)
    var ticketPrefix = "ticket-"
    var ticketIdentifierType = TicketIdentifierType.NUMBER
    var ticketCount = 0
    var tagAfterNoResponse = 3.days.inWholeMilliseconds
    var closeAfterNoResponse = 7.days.inWholeMilliseconds

}

enum class TicketIdentifierType {
    USER_ID,
    NUMBER,
    USER_NAME
}