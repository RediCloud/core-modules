package net.dustrean.modules.discord.data

import com.google.gson.JsonParser
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.dustrean.api.config.IConfig
import net.dustrean.api.utils.extension.gson
import net.dustrean.modules.discord.util.message.useDefaultDesign
import net.dustrean.modules.discord.util.snowflake

abstract class AbstractDiscordConfig : IConfig

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

data class Embed(
    var title: String? = null,
    var description: String? = null,
    var color: IntArray? = null,
    var author: String? = null,
    var authorUrl: String? = null,
    var authorIconUrl: String? = null,
    var url: String? = null,
    var imageUrl: String? = null,
    var thumbnailUrl: String? = null,
    var footer: String? = null,
    var footerIconUrl: String? = null,
    var defaultDesign: Boolean = true,
    val fields: MutableList<EmbedField> = mutableListOf()
)

//Json from https://old.message.style/dashboard
fun toMessage(json: String): Message {
    val element = JsonParser().parse(json)
    val message = Message()
    message.content = element.asJsonObject["content"].asString
    element.asJsonArray.forEach { embedJsonElement ->
        val embed = Embed()
        embed.title = embedJsonElement.asJsonObject["title"].asString
        //embed.color = embedJsonElement.asJsonObject["color"].asInt //TODO
        embed.description = embedJsonElement.asJsonObject["description"].asString
        embed.url = embedJsonElement.asJsonObject["url"].asString
        embedJsonElement.asJsonObject.also {
            embed.author = it["name"].asString
            embed.authorUrl = it["url"].asString
            embed.authorIconUrl = it["icon_url"].asString
        }
        embed.imageUrl = embedJsonElement.asJsonObject["image"].asJsonObject["url"].asString
        embed.thumbnailUrl = embedJsonElement.asJsonObject["thumbnail"].asJsonObject["url"].asString
        embedJsonElement.asJsonObject["footer"].also {
            embed.footer = it.asJsonObject["text"].asString
            embed.footerIconUrl = it.asJsonObject["icon_url"].asString
        }
        embedJsonElement.asJsonObject["fields"].asJsonArray.forEach { fieldJsonElement ->
            val key = fieldJsonElement.asJsonObject["name"].asString
            val value = fieldJsonElement.asJsonObject["value"].asString
            val inline = fieldJsonElement.asJsonObject["inline"].asBoolean
            val field = EmbedField(key, value, inline)
            embed.fields.add(field)
        }
        message.embeds.add(embed)
    }
    return message
}

suspend fun Embed.build(user: User?): EmbedBuilder {
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

data class Message(
    var content: String? = null, val embeds: MutableList<Embed> = mutableListOf(), var defaultDesign: Boolean = true
)

fun Message.embed(block: Embed.() -> Unit) {
    val embed = Embed().apply(block)
    if (defaultDesign) embed.defaultDesign = true
    embeds.add(embed)
}

fun messages(block: Message.() -> Unit): Message = Message().apply(block)

suspend fun MessageChannelBehavior.createMessage(
    message: Message,
    user: User? = null,
    placeholder: MutableMap<String, String> = mutableMapOf(),
    actionRow: ((Int) -> ActionRowBuilder.() -> Unit)? = null
) {
    var i = 1
    createMessage {
        if (!message.content.isNullOrBlank()) content = message.content
        message.embeds.forEach {
            if (actionRow != null) components.add(ActionRowBuilder().apply(actionRow(i)))
            it.build(user).apply {
                if (message.defaultDesign) useDefaultDesign(user)
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
    message: Message,
    user: User? = null,
    placeholder: MutableMap<String, String> = mutableMapOf(),
    actionRow: ((Int) -> ActionRowBuilder.() -> Unit)? = null
) {
    var i = 1
    respondPublic {
        if (!message.content.isNullOrBlank()) content = message.content
        message.embeds.forEach {
            if (actionRow != null) components.add(ActionRowBuilder().apply(actionRow(i)))
            it.build(user).apply {
                if (message.defaultDesign) useDefaultDesign(user)
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
    message: Message,
    user: User? = null,
    placeholder: MutableMap<String, String> = mutableMapOf(),
    actionRow: ((Int) -> ActionRowBuilder.() -> Unit)? = null
) {
    var i = 1
    respondEphemeral {
        if (!message.content.isNullOrBlank()) content = message.content
        message.embeds.forEach {
            if (actionRow != null) components.add(ActionRowBuilder().apply(actionRow(i)))
            it.build(user).apply {
                if (message.defaultDesign) useDefaultDesign(user)
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