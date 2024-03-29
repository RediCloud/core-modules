package dev.redicloud.core.modules.discord.util.message

import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.redicloud.core.modules.discord.kord

fun EmbedBuilder.userFooter(user: User, format: String? = null) = footer {
    icon = user.avatar?.url
    text = format?.replace("%username", user.username)?.replace("%tag", user.discriminator) ?: user.username
}

suspend fun EmbedBuilder.useDefaultDesign(user: User?) {
    useDefaultFooter(user)
    useDefaultColor()
}

fun EmbedBuilder.useDefaultColor() {
    color = Color(135,97,56)
}

suspend fun EmbedBuilder.useDefaultFooter(user: User?) = footer {
    if (user == null) {
        text = "RediCloud • Requested by ${kord.getSelf().username}"
        return@footer
    }
    icon = user.avatar?.url
    text = "RediCloud • Requested by ${user.username}#${user.discriminator}"
}

suspend fun EmbedBuilder.selfFooter() = userFooter(kord.getSelf())