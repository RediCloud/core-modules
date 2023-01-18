package net.dustrean.modules.discord.util.message

import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import net.dustrean.modules.discord.kord

fun EmbedBuilder.userFooter(user: User, format: String? = null) = footer {
    icon = user.avatar?.url
    text = format?.replace("%username", user.username)?.replace("%tag", user.discriminator) ?: user.username
}

fun EmbedBuilder.useDefaultDesign(user: User?) {
    useDefaultFooter(user)
    useDefaultColor()
}

fun EmbedBuilder.useDefaultColor() {
    color = Color(135,97,56)
}

fun EmbedBuilder.useDefaultFooter(user: User?) = footer {
    if (user == null) {
        text = "DustreanNET • Requested by Unknown"
        return@footer
    }
    icon = user.avatar?.url
    text = "DustreanNET • Requested by ${user.username}#${user.discriminator}"
}

suspend fun EmbedBuilder.selfFooter() = userFooter(kord.getSelf())