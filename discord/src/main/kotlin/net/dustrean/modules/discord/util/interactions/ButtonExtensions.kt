package net.dustrean.modules.discord.util.interactions

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import net.dustrean.modules.discord.kord

class InteractionButtonBuilder(val customID: String?) {
    var label: String? = null
    var emoji: DiscordPartialEmoji? = null
    var disabled: Boolean = false
}

inline fun ActionRowBuilder.button(
    style: ButtonStyle, customID: InteractionCommandID, crossinline builder: InteractionButtonBuilder.() -> Unit
) = interactionButton(style, customID.id) {
    val built = InteractionButtonBuilder(customID.id).apply(builder)
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