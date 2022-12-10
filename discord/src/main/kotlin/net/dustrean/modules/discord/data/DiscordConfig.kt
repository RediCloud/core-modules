package net.dustrean.modules.discord.data

data class DiscordConfig(
    val token: String = "xxxxxxxxxxxxxxxxxxxxx",
    val teamDiscordID: String = "938425677345079326",
    val publicDiscordID: String = "789578052924866590",
    val announcementChannelID: String = "",
    val teamAnnouncementCreateChannelID: String = "",
    val bugsChannelID: String = "",
    val closedTicketCategoryID: String = "",
    val openedTicketCategoryID: String = ""
)