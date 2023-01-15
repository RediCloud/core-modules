package net.dustrean.modules.discord.part.impl.moderation

import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.moderation.ModerationRequest
import com.aallam.openai.api.moderation.ModerationResult
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import net.dustrean.modules.discord.configManager
import net.dustrean.modules.discord.kord
import net.dustrean.modules.discord.mainGuild
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder

class ChatModerationPart() : DiscordModulePart() {

    override val name: String = "ChatModeration"
    override val commands: List<CommandBuilder> = listOf()
    private lateinit var openAi: OpenAI
    private lateinit var config: ChatModerationConfig
    private val cache = mutableMapOf<String, List<ModerationResult>>()

    override suspend fun init() {
        openAi = OpenAI(OpenAIConfig(System.getenv("OPENAI_API_KEY"), LogLevel.Info))
        config = if (!configManager.exists("discord-bot:chat-moderation")) {
            val config = ChatModerationConfig()
            configManager.createConfig(config)
            config
        } else {
            configManager.getConfig("discord-bot:chat-moderation", ChatModerationConfig::class.java)
        }
    }

    private val chatModeration = kord.on<MessageCreateEvent> {
        if (message.getGuild() != mainGuild) return@on
        if (message.author?.isBot == true) return@on
        if (!config.chatModeration.contains(message.channelId.value.toLong())) return@on

        if (cache.containsKey(message.content.lowercase())) {
            val list = cache[message.content.lowercase()]!!
            flag(message, getViolations(list))
            return@on
        }
        val request = ModerationRequest(message.content)
        val result = openAi.moderations(request)
        val list = result.results
        flag(message, getViolations(list))
        cache[message.content.lowercase()] = list
    }

    private suspend fun flag(message: Message, violations: Set<String>) {
        if(violations.isEmpty()) return
        val author = message.author ?: return
        val dmChannel = author.getDmChannelOrNull() ?: return
        dmChannel.createMessage {
            embed {
                title = "Chat Moderation | DustreanNET"
                color = Color(250, 0, 0)
                description = "Your messsage: ```${message.content}``` was flagged for the following ${if(violations.size == 1) "violation" else "violations"}: ${violations.joinToString(", ")}"
            }
        }
        message.delete("Message was flagged as ${violations.joinToString(", ")}")
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

    private val logEdits = kord.on<MessageUpdateEvent> {

    }

}