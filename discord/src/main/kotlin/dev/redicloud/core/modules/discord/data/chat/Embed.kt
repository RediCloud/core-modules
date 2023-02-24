package dev.redicloud.core.modules.discord.data.chat

import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign

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

fun Message.embed(block: Embed.() -> Unit) {
    val embed = Embed().apply(block)
    if (defaultDesign) embed.defaultDesign = true
    embeds.add(embed)
}

fun Embed.field(block: EmbedField.() -> Unit): EmbedField = EmbedField(
    "",
    ""
).apply(block)

suspend fun Embed.build(user: User?): EmbedBuilder {
    val builder = EmbedBuilder()
    builder.title = title
    builder.description = description
    builder.author {
        name = author
        url = authorUrl
        icon = authorIconUrl
    }
    builder.url = url
    builder.image = imageUrl
    thumbnailUrl?.let {
        if (it.isBlank()) return@let
        builder.thumbnail {
            url = it
        }
    }
    builder.footer {
        if (!footer.isNullOrBlank()) text = footer!!
        if (!footerIconUrl.isNullOrBlank()) icon = footerIconUrl
    }
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