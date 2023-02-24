package dev.redicloud.core.modules.discord.util.interactions

import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.SelectOptionBuilder
import dev.redicloud.core.modules.discord.kord

class SelectionMenuBuilder(val customID: String) {
    var allowedValues: ClosedRange<Int> = 1..1
    var options: MutableList<SelectOptionBuilder> = mutableListOf()
    var placeholder: String? = null
    var disabled: Boolean = false

    inline fun perform(crossinline block: SelectMenuInteractionCreateEvent.() -> Unit) =
        kord.on<SelectMenuInteractionCreateEvent> {
            if (interaction.component.customId != customID) return@on
            block()
        }
}

inline fun ActionRowBuilder.selectionMenu(
    customId: String, crossinline builder: SelectionMenuBuilder.() -> Unit
) = selectMenu(customId) {
    val built = SelectionMenuBuilder(customId).apply(builder)
    allowedValues = built.allowedValues
    options += built.options
    placeholder = built.placeholder
    disabled = built.disabled
}

inline fun ActionRowBuilder.selectionMenu(
    customId: Enum<*>, crossinline builder: SelectionMenuBuilder.() -> Unit
) = selectMenu(customId.name) {
    val built = SelectionMenuBuilder(customId.name).apply(builder)
    allowedValues = built.allowedValues
    options += built.options
    placeholder = built.placeholder
    disabled = built.disabled
}