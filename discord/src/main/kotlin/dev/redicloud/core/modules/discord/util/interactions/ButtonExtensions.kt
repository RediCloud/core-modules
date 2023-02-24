package dev.redicloud.core.modules.discord.util.interactions

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.rest.builder.component.ActionRowBuilder

class InteractionButtonBuilder(val customID: String?) {
    var label: String? = null
    var emoji: DiscordPartialEmoji? = null
    var disabled: Boolean = false
}

inline fun ActionRowBuilder.button(
    style: ButtonStyle, customID: Enum<*>, crossinline builder: InteractionButtonBuilder.() -> Unit
) = interactionButton(style, customID.name) {
    val built = InteractionButtonBuilder(customID.name).apply(builder)
    label = built.label
    emoji = built.emoji
    disabled = built.disabled
}

inline fun ActionRowBuilder.button(
    style: ButtonStyle, customId: String, crossinline builder: InteractionButtonBuilder.() -> Unit
) = interactionButton(style, customId) {
    val built = InteractionButtonBuilder(customId).apply(builder)
    label = built.label
    emoji = built.emoji
    disabled = built.disabled
}

inline fun ActionRowBuilder.linkButton(url: String, crossinline builder: InteractionButtonBuilder.() -> Unit) =
    linkButton(url) {
        val built = InteractionButtonBuilder(null).apply(builder)
        label = built.label
        emoji = built.emoji
        disabled = built.disabled
    }