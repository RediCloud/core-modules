package net.dustrean.modules.discord.part.impl.moderation

import net.dustrean.api.config.IConfig

class ChatModerationConfig : IConfig {
    override val key = "discord-bot:chat-moderation"

    val channels = mutableSetOf<Long>()
}