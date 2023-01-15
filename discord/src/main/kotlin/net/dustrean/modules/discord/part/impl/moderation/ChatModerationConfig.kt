package net.dustrean.modules.discord.part.impl.moderation

import net.dustrean.api.config.IConfig

class ChatModerationConfig : IConfig {

    override val key = "discord-bot:chat-moderation"

    val autoModeration = true
    val addNewChannelsToModeration = true
    val moderateOtherBots = false
    val chatModeration = mutableSetOf(1063220865594036234L)

    val logEdits = true
    val addNewChannelsToEdits = true
    val logBotEdits = false
    val logEditsInChannels = mutableSetOf(1063220865594036234L)

    val logDeletes = true
    val addNewChannelsToDeletes = true
    val logDeletesByBots = false
    val logDeletesInChannels = mutableSetOf(1063220865594036234L)

    val logChannel = 1047935742971748382L

}