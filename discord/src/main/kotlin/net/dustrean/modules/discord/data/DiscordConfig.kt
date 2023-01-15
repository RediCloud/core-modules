package net.dustrean.modules.discord.data

import net.dustrean.api.config.IConfig

data class DiscordConfig(
    override val key: String = "discord:bot",
    var teamDiscordID: String = "938425677345079326",
    var publicDiscordID: String = "938425677345079326",
    var logChannel: Long = 1047935742971748382L
) : IConfig