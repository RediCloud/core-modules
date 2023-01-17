package net.dustrean.modules.discord.data

import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import net.dustrean.api.config.IConfig
import net.dustrean.modules.discord.util.message.useDefaultFooter

abstract class AbstractDiscordConfig : IConfig {

    data class Emoji(
        val id: Long?, val name: String?, val animated: Boolean = false
    )

    data class Embed(
        val title: String? = null,
        val description: String? = null,
        val color: IntArray? = null,
        val useDefaultFooter: Boolean = true,
        val fields: MutableList<EmbedField> = mutableListOf()
    ) {
        fun build(user: User): EmbedBuilder {
            val builder = EmbedBuilder()
            builder.title = title
            builder.description = description
            color?.let {
                builder.color = Color(color.get(0), color.get(1), color.get(2))
            }
            if (useDefaultFooter) builder.useDefaultFooter(user)
            fields.forEach {
                builder.field {
                    name = it.name
                    value = it.value
                    inline = it.inline
                }
            }
            return builder
        }
    }

    data class EmbedField(
        val name: String, val value: String, val inline: Boolean = false
    )

}