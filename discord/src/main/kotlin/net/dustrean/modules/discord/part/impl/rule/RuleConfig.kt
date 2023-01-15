package net.dustrean.modules.discord.part.impl.rule

import dev.kord.common.Color
import net.dustrean.api.config.IConfig

class RuleConfig() : IConfig {
    override val key: String = "discord:modules:rule"
    val ruleMessages: List<Rule> = listOf()
    val acceptEmoji = "✅"
    val acceptRole = 1064179288552787969L
}

data class Rule(val color: Int = 0xFFFFF, val title: String = "", val description: String = "")