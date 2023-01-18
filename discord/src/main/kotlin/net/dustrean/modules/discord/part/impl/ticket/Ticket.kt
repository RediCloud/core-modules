package net.dustrean.modules.discord.part.impl.ticket

import java.util.*

class Ticket {

    var id = UUID.randomUUID()
    var channelId = 0L
    var stateHistory = mutableMapOf<Long, Pair<TicketState, Long>>()
    var lastCreatorMessage = 0L

    fun isOpen(): Boolean =
        stateHistory.values.last().first == TicketState.OPENED || stateHistory.values.last().first == TicketState.REOPENED

    val creatorId: Long
        get() = stateHistory.values.first { it.first == TicketState.OPENED }.second

    fun update() {
        TicketPart.tickets[id] = this
    }

}

enum class TicketState {
    OPENED,
    CLOSED,
    REOPENED,
    DELETED
}
