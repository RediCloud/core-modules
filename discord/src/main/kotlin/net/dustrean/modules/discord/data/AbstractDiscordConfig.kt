package net.dustrean.modules.discord.data

import dev.kord.common.Color
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.User
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import net.dustrean.api.config.IConfig
import net.dustrean.modules.discord.part.impl.rule.RulePart
import net.dustrean.modules.discord.util.message.useDefaultDesign
import net.dustrean.modules.discord.util.snowflake

abstract class AbstractDiscordConfig : IConfig

data class Emoji(
    val id: Long?, val name: String?, val animated: Boolean = false
) {
    fun mention(): String = if (animated) {
        "<a:$name:$id>"
    } else if (id != null && name != null) {
        "<:$name:$id>"
    } else {
        "$name"
    }
    fun partialEmoji(): DiscordPartialEmoji = DiscordPartialEmoji(
        id?.snowflake,
        name,
        OptionalBoolean.Value(animated)
    )
}

fun emoji(block: Emoji.() -> Unit): Emoji = Emoji(null, null).apply(block)

data class Embed(
    var title: String? = null,
    var description: String? = null,
    var color: IntArray? = null,
    var defaultDesign: Boolean = true,
    val fields: MutableList<EmbedField> = mutableListOf()
)
fun Embed.build(user: User?): EmbedBuilder {
    val builder = EmbedBuilder()
    builder.title = title
    builder.description = description
    color?.let {
        builder.color = Color(it[0], it[1], it[2])
    }
    if (defaultDesign) builder.useDefaultDesign(user)
    fields.forEach {
        builder.field {
            name = it.name
            value = it.value
            inline = it.inline
        }
    }
    return builder
}

data class EmbedField(
    var name: String, var value: String, var inline: Boolean = false
)

fun Embed.field(block: EmbedField.() -> Unit): EmbedField = EmbedField("", "").apply(block)

data class Messages(val embeds: MutableList<Embed> = mutableListOf(), var defaultDesign: Boolean = true)
fun Messages.embed(block: Embed.() -> Unit) {
    val embed = Embed().apply(block)
    if (defaultDesign) embed.defaultDesign = true
    embeds.add(embed)
}

fun messages(block: Messages.() -> Unit): Messages = Messages().apply(block)

suspend fun MessageChannelBehavior.createMessage(
    messages: Messages,
    user: User? = null,
    placeholder: MutableMap<String, String> = mutableMapOf(),
    actionRow: ((Int) -> ActionRowBuilder.() -> Unit)? = null
) {
    var i = 1
    messages.embeds.forEach {
        createMessage {
            if (actionRow != null) components.add(ActionRowBuilder().apply(actionRow(i)))
            it.build(user).apply {
                if (messages.defaultDesign) useDefaultDesign(user)
                if (it.defaultDesign) useDefaultDesign(user)
                placeholder.forEach { (key, value) ->
                    title = title?.replace("{$key}", value)
                    description = description?.replace("{$key}", value)
                    fields.forEach { field ->
                        field.name = field.name.replace("{$key}", value)
                        field.value = field.value.replace("{$key}", value)
                    }
                }
            }.also { embeds.add(it) }
        }
        i++
    }
}

suspend fun ActionInteractionBehavior.respondPublic(
    messages: Messages,
    user: User? = null,
    placeholder: MutableMap<String, String> = mutableMapOf(),
    actionRow: ((Int) -> ActionRowBuilder.() -> Unit)? = null
) {
    var i = 1
    messages.embeds.forEach {
        respondPublic {
            if (actionRow != null) components.add(ActionRowBuilder().apply(actionRow(i)))
            it.build(user).apply {
                if (messages.defaultDesign) useDefaultDesign(user)
                if (it.defaultDesign) useDefaultDesign(user)
                placeholder.forEach { (key, value) ->
                    title = title?.replace("{$key}", value)
                    description = description?.replace("{$key}", value)
                    fields.forEach { field ->
                        field.name = field.name.replace("{$key}", value)
                        field.value = field.value.replace("{$key}", value)
                    }
                }
            }.also { embeds.add(it) }
        }
        i++
    }
}

suspend fun ActionInteractionBehavior.respondEphemeral(
    messages: Messages,
    user: User? = null,
    placeholder: MutableMap<String, String> = mutableMapOf(),
    actionRow: ((Int) -> ActionRowBuilder.() -> Unit)? = null
) {
    var i = 1
    messages.embeds.forEach {
        respondEphemeral {
            if (actionRow != null) components.add(ActionRowBuilder().apply(actionRow(i)))
            it.build(user).apply {
                if (messages.defaultDesign) useDefaultDesign(user)
                if (it.defaultDesign) useDefaultDesign(user)
                placeholder.forEach { (key, value) ->
                    title = title?.replace("{$key}", value)
                    description = description?.replace("{$key}", value)
                    fields.forEach { field ->
                        field.name = field.name.replace("{$key}", value)
                        field.value = field.value.replace("{$key}", value)
                    }
                }
            }.also { embeds.add(it) }
        }
        i++
    }
}