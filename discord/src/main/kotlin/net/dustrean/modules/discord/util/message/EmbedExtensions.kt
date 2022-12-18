package net.dustrean.modules.discord.util.message

import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import net.dustrean.modules.discord.kord

fun EmbedBuilder.userFooter(user: User, format: String? = null) = footer {
    icon = user.avatar?.url
    text = format?.replace("%username", user.username)?.replace("%tag", user.discriminator) ?: user.username
}

suspend fun EmbedBuilder.selfFooter() = userFooter(kord.getSelf())