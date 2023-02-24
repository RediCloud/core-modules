package dev.redicloud.core.modules.discord.part.impl.rule

import dev.redicloud.core.modules.discord.data.AbstractDiscordConfig
import dev.redicloud.core.modules.discord.data.chat.Emoji
import dev.redicloud.core.modules.discord.data.chat.Message
import dev.redicloud.core.modules.discord.data.chat.embed
import dev.redicloud.core.modules.discord.data.chat.message

class RuleConfig() : AbstractDiscordConfig() {
    override val key: String = "discord:modules:rule"
    var ruleMessage: Message = message {
        embed {
            defaultDesign = true
            title = "Rules | DustreanNET"
            description = "The discord Terms of Service and Community Guidelines apply to this server. Please read them carefully."
        }
    }
    var acceptEmoji: Emoji = Emoji(name = "✅")
    var acceptRole = 1064179288552787969L
}