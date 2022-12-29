package net.dustrean.modules.discord.data

import net.dustrean.api.config.IConfig

data class DiscordConfig(
    override val key: String = "discord-bot",
    val teamDiscordID: String = "938425677345079326",
    val publicDiscordID: String = "789578052924866590",
    val channels: Channels = Channels(),
    val roles: Roles = Roles(),
    val ruleMessages: List<Rule> = listOf()
) : IConfig

data class Channels(
    val announcementChannelID: String = "",
    val teamAnnouncementCreateChannelID: String = "",
    val bugsChannelID: String = "",
    val closedTicketCategoryID: String = "",
    val openedTicketCategoryID: String = ""
)

data class Roles(val playerID: String = "")

data class Rule(val color: Int = 0xFFFFF, val title: String = "", val description: String = "")