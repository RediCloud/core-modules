package net.dustrean.modules.discord.util.interactions

import dev.kord.common.entity.TextInputStyle
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import net.dustrean.modules.discord.kord

class PopUpBuilder(val customID: String) {
    var length: ClosedRange<Int>? = null
    var placeholder: String? = null
    var value: String? = null
    var required: Boolean = true
    var disabled: Boolean = false

    inline fun perform(crossinline callback: suspend ModalSubmitInteractionCreateEvent.() -> Unit) =
        kord.on<ModalSubmitInteractionCreateEvent> {
            if (interaction.modalId != customID) return@on
            callback()
        }
}

inline fun ActionRowBuilder.popUp(
    style: TextInputStyle, customID: InteractionCommandID, label: String, crossinline builder: PopUpBuilder.() -> Unit
) = textInput(style, customID.id, label) {
    val built = PopUpBuilder(customID.id).apply(builder)
    allowedLength = built.length
    placeholder = built.placeholder
    value = built.value
    required = built.required
    disabled = built.disabled
}