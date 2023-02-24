package dev.redicloud.core.modules.discord.data.chat

import com.google.gson.JsonParser
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.User
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign

data class Message(
    var content: String? = null, val embeds: MutableList<Embed> = mutableListOf(), var defaultDesign: Boolean = true
)

fun message(block: Message.() -> Unit): Message = Message().apply(block)

//Json from https://message.style
fun toMessage(json: String): Message {
    val element = JsonParser().parse(json)
    val message = Message()
    message.content = element.asJsonObject["content"]?.asString
    element.asJsonObject["embeds"]?.asJsonArray?.forEach { embedJsonElement ->
        val embed = Embed()
        embed.title = embedJsonElement.asJsonObject["title"]?.asString
        //embed.color = embedJsonElement.asJsonObject["color"].asInt //TODO
        embed.description = embedJsonElement.asJsonObject["description"]?.asString
        embed.url = embedJsonElement.asJsonObject["url"]?.asString
        embedJsonElement.asJsonObject.also {
            embed.author = it["name"]?.asString
            embed.authorUrl = it["url"]?.asString
            embed.authorIconUrl = it["icon_url"]?.asString
        }
        embed.imageUrl = embedJsonElement.asJsonObject["image"]?.asJsonObject?.get("url")?.asString
        embed.thumbnailUrl = embedJsonElement.asJsonObject["thumbnail"]?.asJsonObject?.get("url")?.asString
        embedJsonElement.asJsonObject["footer"]?.also {
            embed.footer = it.asJsonObject["text"]?.asString
            embed.footerIconUrl = it.asJsonObject["icon_url"]?.asString
        }
        embedJsonElement.asJsonObject["fields"]?.asJsonArray?.forEach { fieldJsonElement ->
            val key = fieldJsonElement.asJsonObject["name"]?.asString
            val value = fieldJsonElement.asJsonObject["value"]?.asString
            val inline = fieldJsonElement.asJsonObject["inline"]?.asBoolean ?: false
            if (key == null || value == null) return@forEach
            val field = EmbedField(key, value, inline)
            embed.fields.add(field)
        }
        message.embeds.add(embed)
    }
    return message
}

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