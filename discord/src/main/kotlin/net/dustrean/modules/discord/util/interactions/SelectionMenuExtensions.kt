package net.dustrean.modules.discord.util.interactions

import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.SelectOptionBuilder
import net.dustrean.modules.discord.kord

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
    customID: InteractionCommandID, crossinline builder: SelectionMenuBuilder.() -> Unit
) = selectMenu(customID.id) {
    val built = SelectionMenuBuilder(customID.id).apply(builder)
    allowedValues = built.allowedValues
    options += built.options
    placeholder = built.placeholder
    disabled = built.disabled
}