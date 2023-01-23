package net.dustrean.modules.discord.part.impl.rule

import net.dustrean.modules.discord.data.AbstractDiscordConfig
import net.dustrean.modules.discord.data.chat.Emoji

class RuleConfig() : AbstractDiscordConfig() {
    override val key: String = "discord:modules:rule"
    val ruleMessages: List<Rule> = listOf()
    var acceptEmoji: Emoji = Emoji(name = "✅")
    var acceptRole = 1064179288552787969L
}

data class Rule(val color: Int = 0xFFFFF, val title: String = "", val description: String = "")