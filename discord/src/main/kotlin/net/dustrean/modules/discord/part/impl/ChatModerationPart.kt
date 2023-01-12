package net.dustrean.modules.discord.part.impl

import com.aallam.openai.api.moderation.ModerationRequest
import com.aallam.openai.api.moderation.ModerationResult
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import net.dustrean.modules.discord.kord
import net.dustrean.modules.discord.mainGuild
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder

class ChatModerationPart() : DiscordModulePart() {

    override val name: String = "ChatModeration"
    override val commands: List<CommandBuilder> = listOf()
    private lateinit var openAi: OpenAI
    private val cache = mutableMapOf<String, List<ModerationResult>>()

    override suspend fun init() {
        val config = OpenAIConfig(System.getenv("OPENAI_API_KEY"))
        openAi = OpenAI(config)
    }

    private val chat = kord.on<MessageCreateEvent> {
        if (message.getGuild() != mainGuild) return@on
        if (cache.containsKey(message.content.lowercase())) {
            val list = cache[message.content.lowercase()]!!
            val violations = getViolations(list)
            if(violations.isNotEmpty()) {
                message.channel.createMessage("Your message was deleted because it was flagged as ${violations.joinToString(", ")}")
                message.delete("Message was flagged as ${violations.joinToString(", ")}")
            }
            return@on
        }
        val request = ModerationRequest(message.content)
        val result = openAi.moderations(request)
        val list = result.results
        val violations = getViolations(list)
        if(violations.isNotEmpty()) {
            message.channel.createMessage("Your message was deleted because it was flagged as ${violations.joinToString(", ")}")
            message.delete("Message was flagged as ${violations.joinToString(", ")}")
        }
        cache[message.content.lowercase()] = list
    }

    private fun getViolations(list: List<ModerationResult>): Set<String> {
        val violation = mutableSetOf<String>()
        list.forEach {
            if (it.flagged) {
                if(it.categories.hate) violation.add("Hate")
                if(it.categories.hateThreatening) violation.add("Hate Threatening")
                if(it.categories.sexual) violation.add("Sexual")
                if(it.categories.violence) violation.add("Violence")
                if(it.categories.selfHarm) violation.add("Self Harm")
                if(it.categories.sexualMinors) violation.add("Sexual Minors")
                if(it.categories.violenceGraphic) violation.add("Violence Graphic")
            }
        }
        return violation
    }

}