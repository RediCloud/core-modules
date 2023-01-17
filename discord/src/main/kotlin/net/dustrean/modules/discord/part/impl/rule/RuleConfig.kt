package net.dustrean.modules.discord.part.impl.rule

import net.dustrean.modules.discord.data.AbstractDiscordConfig

class RuleConfig() : AbstractDiscordConfig() {
    override val key: String = "discord:modules:rule"
    val ruleMessages: List<Rule> = listOf()
    var acceptEmoji: Emoji = Emoji(null, "✅")
    var acceptRole = 1064179288552787969L
    val acceptMessages = mutableListOf<Long>()
}

data class Rule(val color: Int = 0xFFFFF, val title: String = "", val description: String = "")