package dev.redicloud.core.modules.discord.data.chat

data class EmbedField(
    var name: String, var value: String, var inline: Boolean = false
)