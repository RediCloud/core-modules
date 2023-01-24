package net.dustrean.modules.discord.data.chat

import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.optional.OptionalBoolean
import net.dustrean.modules.discord.util.snowflake

data class Emoji(
    val id: Long? = null, val name: String? = null, val animated: Boolean = false
) {
    fun mention(): String = if (animated) {
        "<a:$name:$id>"
    } else if (id != null && name != null) {
        "<:$name:$id>"
    } else {
        "$name"
    }

    fun partialEmoji(): DiscordPartialEmoji = DiscordPartialEmoji(
        id?.snowflake, name, OptionalBoolean.Value(animated)
    )
}

fun emoji(block: Emoji.() -> Unit): Emoji = Emoji().apply(block)