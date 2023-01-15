package net.dustrean.modules.discord.part.impl.moderation

import net.dustrean.api.config.IConfig

class ModerationConfig : IConfig {

    override val key = "discord:modules:moderation"

    var autoModeration = true
    var addNewChannelsToAutoModeration = true
    var moderateOtherBots = false
    val chatModerationChannels = mutableSetOf(1063220865594036234L)

    var logEdits = true
    var addNewChannelsToLogEdits = true
    var logBotEdits = false
    val logEditsInChannels = mutableSetOf(1063220865594036234L)

    var logDeletes = true
    var addNewChannelsToDeletes = true
    var logDeletesByBots = false
    var logDeletesInChannels = mutableSetOf(1063220865594036234L)

}