package dev.redicloud.core.modules.discord.part.impl.moderation

import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.moderation.ModerationRequest
import com.aallam.openai.api.moderation.ModerationResult
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import dev.kord.common.Color
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.channel.TextChannelCreateEvent
import dev.kord.core.event.channel.TextChannelDeleteEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.message.create.embed
import dev.redicloud.api.utils.networkName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import dev.redicloud.core.modules.discord.*
import dev.redicloud.core.modules.discord.part.DiscordModulePart
import dev.redicloud.core.modules.discord.util.commands.CommandBuilder
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign
import dev.redicloud.core.modules.discord.util.snowflake

object ModerationPart : DiscordModulePart() {

    override val name: String = "Moderation"
    override val commands: List<CommandBuilder> = listOf()
    private lateinit var openAi: OpenAI
    private lateinit var config: ModerationConfig
    private val cache = mutableMapOf<String, List<ModerationResult>>()

    override suspend fun init() {
        openAi = OpenAI(OpenAIConfig(System.getenv("OPENAI_API_KEY"), LogLevel.None))
        config = if (!configManager.exists("discord:modules:moderation")) {
            val config = ModerationConfig()
            configManager.createConfig(config)
            config
        } else {
            configManager.getConfig("discord:modules:moderation", ModerationConfig::class.java)
        }
        loadConfigCommand()
    }

    private val chatModeration = kord.on<MessageCreateEvent> {
        if (message.author?.isBot == true && !config.moderateOtherBots) return@on
        if (!config.chatModerationChannels.contains(message.channelId.value.toLong())) return@on
        if (message.content.startsWith("/")) return@on

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
        if (violations.isEmpty()) return
        logChannel.createMessage {
            embed {
                title = "Auto Moderation | $networkName"
                description =
                    "Message sent by ${message.author?.mention} in ${message.channel.asChannelOf<TextChannel>().mention} was flagged for the following reasons: ${
                        violations.joinToString(", ")
                    }"
                color = Color(250, 0, 0)
                field {
                    name = "Message"
                    value = message.content
                }
            }
        }
        val author = message.author ?: return
        val dmChannel = author.getDmChannelOrNull() ?: return
        dmChannel.createMessage {
            embed {
                title = "Chat Moderation | $networkName"
                color = Color(250, 0, 0)
                description =
                    "Your messsage: ```${message.content}``` was flagged for the following ${if (violations.size == 1) "violation" else "violations"}: ${
                        violations.joinToString(", ")
                    }"
            }
        }
        message.delete("Message was flagged as ${violations.joinToString(", ")}")
    }

    private fun getViolations(list: List<ModerationResult>): Set<String> {
        val violation = mutableSetOf<String>()
        list.forEach {
            if (it.flagged) {
                if (it.categories.hate) violation.add("Hate")
                if (it.categories.hateThreatening) violation.add("Hate Threatening")
                if (it.categories.sexual) violation.add("Sexual")
                if (it.categories.violence) violation.add("Violence")
                if (it.categories.selfHarm) violation.add("Self Harm")
                if (it.categories.sexualMinors) violation.add("Sexual Minors")
                if (it.categories.violenceGraphic) violation.add("Violence Graphic")
            }
        }
        return violation
    }

    private val channelCreate = kord.on<TextChannelCreateEvent> {
        if (config.addNewChannelsToDeletes) {
            config.logDeletesInChannels.add(channel.id.value.toLong())
        }
        if (config.addNewChannelsToLogEdits) {
            config.logEditsInChannels.add(channel.id.value.toLong())
        }
        if (config.addNewChannelsToAutoModeration) {
            config.chatModerationChannels.add(channel.id.value.toLong())
        }
    }

    private val channelDelete = kord.on<TextChannelDeleteEvent> {
        config.logDeletesInChannels.remove(channel.id.value.toLong())
        config.logEditsInChannels.remove(channel.id.value.toLong())
        config.chatModerationChannels.remove(channel.id.value.toLong())
    }

    private val logEdits = kord.on<MessageUpdateEvent> {
        if (!config.logEditsInChannels.contains(message.channelId.value.toLong())) return@on
        if (message.asMessage().author?.isBot == true && !config.logBotEdits) return@on

        val oldMessage = old?.asMessageOrNull() ?: return@on
        val newMessage = message.asMessageOrNull() ?: return@on

        logChannel.asChannelOf<TextChannel>().createMessage {
            embed {
                title = "Message Edited | $networkName"
                color = Color(250, 0, 0)
                url = "https://discord.com/channels/${
                    message.asMessage().getGuildOrNull()?.id?.value
                }/${message.channelId.value}/${message.id.value}"
                description = "Message edited in ${message.channel.mention} by ${message.asMessage().author?.mention}"
                field {
                    name = "Old Content"
                    value = "```${oldMessage.content}```"
                }
                field {
                    name = "New Content"
                    value = "```${newMessage.content}```"
                }
            }
        }
    }

    private val logDeletes = kord.on<MessageDeleteEvent> {
        if (!config.logDeletesInChannels.contains(messageId.value.toLong())) return@on

        logChannel.asChannelOf<TextChannel>().createMessage {
            embed {
                title = "Message Deleted | $networkName"
                color = Color(250, 0, 0)
                description = "Message deleted in ${channel.mention} from ${message?.author?.mention}"
                field {
                    name = "Content"
                    value = "```${message?.content}```"
                }
            }
        }
    }

    private fun loadConfigCommand() {
        DiscordModule.INSTANCE.configCommands.forEach { it ->
            it.value.apply {
                group("auto-chat-moderation", "Configure the auto chat moderation") {
                    subCommand("state", "Toggle auto chat moderation") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.autoModeration = !config.autoModeration
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Auto Chat Moderation | $networkName"
                                        description =
                                            "Auto chat moderation is now ${if (config.autoModeration) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("add-new-channels", "Toggle if new channels should be moderated automatically") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.addNewChannelsToAutoModeration = !config.addNewChannelsToAutoModeration
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Auto Chat Moderation | $networkName"
                                        description =
                                            "Auto chat moderation for new channels is now ${if (config.addNewChannelsToAutoModeration) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("other-bots", "Toggle if other bots should be moderated") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.moderateOtherBots = !config.moderateOtherBots
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Auto Chat Moderation | $networkName"
                                        description =
                                            "Auto chat moderation for other bots is now ${if (config.moderateOtherBots) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("add", "Add a new channel to auto chat moderation") {
                        channel("channel", "The channel to add") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                if (config.chatModerationChannels.contains(channel.id.value.toLong())) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Auto Chat Moderation | $networkName"
                                            description = "Channel is already in auto chat moderation"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                config.chatModerationChannels.add(channel.id.value.toLong())
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Auto Chat Moderation | $networkName"
                                        description = "Channel ${channel.mention} added to auto chat moderation"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("remove", "Remove a channel from auto chat moderation") {
                        channel("channel", "The channel to remove") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                if (!config.chatModerationChannels.contains(channel.id.value.toLong())) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Auto Chat Moderation | $networkName"
                                            description = "Channel is not in auto chat moderation"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                config.chatModerationChannels.remove(channel.id.value.toLong())
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Auto Chat Moderation | $networkName"
                                        description = "Channel ${channel.mention} removed from auto chat moderation"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("current", "Get current auto chat moderation settings") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Auto Chat Moderation | $networkName"
                                        description =
                                            "Auto chat moderation is a feature that automatically moderates messages in the specified channels."
                                        field {
                                            name = "Auto moderation (via OpenAI)"
                                            value = "Enabled: ${config.autoModeration}"
                                        }
                                        field {
                                            name = "Add new channels to auto moderation"
                                            value = "Enabled: ${config.addNewChannelsToAutoModeration}"
                                        }
                                        field {
                                            name = "Moderate other bots"
                                            value = "Enabled: ${config.moderateOtherBots}"
                                        }
                                        field {
                                            name = "Auto moderation channels"
                                            value = if (config.chatModerationChannels.isEmpty()) "No channels"
                                            else config.chatModerationChannels.joinToString(", ") {
                                                runBlocking {
                                                    kord.getChannel(it.snowflake)?.mention ?: "Unknown channel ($it)"
                                                }
                                            }
                                        }
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                }




                group("log-message-edits", "Configure the log message edits") {
                    subCommand("state", "Toggle log message edits") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.logEdits = !config.logEdits
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Edits | $networkName"
                                        description =
                                            "Log message edits is now ${if (config.logEdits) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("add-new-channels", "Toggle if new channels should be logged automatically") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.addNewChannelsToLogEdits = !config.addNewChannelsToLogEdits
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Edits | $networkName"
                                        description =
                                            "Log message edits for new channels is now ${if (config.addNewChannelsToLogEdits) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("other-bots", "Toggle if other bots should be logged") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.logBotEdits = !config.logBotEdits
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Edits | $networkName"
                                        description =
                                            "Log message edits for other bots is now ${if (config.logBotEdits) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("add", "Add a new channel to log message edits") {
                        channel("channel", "The channel to add") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                if (config.logEditsInChannels.contains(channel.id.value.toLong())) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Log Message Edits | $networkName"
                                            description = "Channel is already in log message edits"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                config.logEditsInChannels.add(channel.id.value.toLong())
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Edits | $networkName"
                                        description = "Channel ${channel.mention} added to log message edits"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("remove", "Remove a channel from log message edits") {
                        channel("channel", "The channel to remove") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                if (!config.logEditsInChannels.contains(channel.id.value.toLong())) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Log Message Edits | $networkName"
                                            description = "Channel is not in log message edits"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                config.logEditsInChannels.remove(channel.id.value.toLong())
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Edits | $networkName"
                                        description = "Channel ${channel.mention} removed from log message edits"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("current", "Get current log message edit settings") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Edits | $networkName"
                                        description =
                                            "Log message edits is a feature that logs all message edits in the specified channels."
                                        field {
                                            name = "Log message edits"
                                            value = "Enabled: ${config.logEdits}"
                                        }
                                        field {
                                            name = "Log bot edits"
                                            value = "Enabled: ${config.logBotEdits}"
                                        }
                                        field {
                                            name = "Log message edits channels"
                                            value = if (config.logEditsInChannels.isEmpty()) "No channels"
                                            else config.logEditsInChannels.joinToString(", ") {
                                                runBlocking {
                                                    kord.getChannel(it.snowflake)?.mention ?: "Unknown channel ($it)"
                                                }
                                            }
                                        }
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                }
                group("log-message-deletes", "Configure the log message deletes") {
                    subCommand("state", "Toggle log message deletes") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.logDeletes = !config.logDeletes
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Deletes | $networkName"
                                        description =
                                            "Log message deletes is now ${if (config.logDeletes) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("add-new-channels", "Toggle if new channels should be logged automatically") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.addNewChannelsToDeletes = !config.addNewChannelsToDeletes
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Deletes | $networkName"
                                        description =
                                            "Log message deletes for new channels is now ${if (config.addNewChannelsToDeletes) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("other-bots", "Toggle if other bots should be logged") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                config.logDeletes = !config.logDeletes
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Deletes | $networkName"
                                        description =
                                            "Log message deletes for other bots is now ${if (config.logDeletes) "enabled" else "disabled"}"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("add", "Add a new channel to log message deletes") {
                        channel("channel", "The channel to add") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                if (config.logDeletesInChannels.contains(channel.id.value.toLong())) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Log Message Deletes | $networkName"
                                            description = "Channel is already in log message deletes"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                config.logDeletesInChannels.add(channel.id.value.toLong())
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Deletes | $networkName"
                                        description = "Channel ${channel.mention} added to log message deletes"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("remove", "Remove a channel from log message deletes") {
                        channel("channel", "The channel to remove") {
                            required = true
                        }
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                val channel = interaction.command.channels["channel"]!!
                                if (!config.logDeletesInChannels.contains(channel.id.value.toLong())) {
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Log Message Deletes | $networkName"
                                            description = "Channel is not in log message deletes"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                    return@launch
                                }
                                config.logDeletesInChannels.remove(channel.id.value.toLong())
                                configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Deletes | $networkName"
                                        description = "Channel ${channel.mention} removed from log message deletes"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                    subCommand("current", "Get current log message delete settings") {
                        perform(this@group, this@subCommand) {
                            ioScope.launch {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Log Message Deletes | $networkName"
                                        description =
                                            "Log message deletes is a feature that logs all message deletes in the specified channels."
                                        field {
                                            name = "Log message deletes"
                                            value = "Enabled: ${config.logDeletes}"
                                        }
                                        field {
                                            name = "Log message deletes channels"
                                            value = if (config.logDeletesInChannels.isEmpty()) "No channels"
                                            else config.logDeletesInChannels.joinToString(", ") {
                                                runBlocking {
                                                    kord.getChannel(it.snowflake)?.mention ?: "Unknown channel ($it)"
                                                }
                                            }
                                        }
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}