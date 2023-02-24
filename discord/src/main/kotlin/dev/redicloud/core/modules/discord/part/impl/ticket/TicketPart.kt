package dev.redicloud.core.modules.discord.part.impl.ticket

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.redicloud.api.ICoreAPI
import dev.redicloud.api.utils.ExceptionHandler.haste
import dev.redicloud.api.utils.extension.ExternalRMap
import dev.redicloud.api.utils.extension.getExternalMap
import dev.redicloud.api.utils.networkName
import dev.redicloud.core.modules.discord.DiscordModuleMain
import dev.redicloud.core.modules.discord.data.chat.createMessage
import dev.redicloud.core.modules.discord.data.chat.respondEphemeral
import dev.redicloud.core.modules.discord.data.chat.respondPublic
import dev.redicloud.core.modules.discord.ioScope
import dev.redicloud.core.modules.discord.kord
import dev.redicloud.core.modules.discord.mainGuild
import dev.redicloud.core.modules.discord.part.DiscordModulePart
import dev.redicloud.core.modules.discord.util.commands.CommandBuilder
import dev.redicloud.core.modules.discord.util.commands.inputCommand
import dev.redicloud.core.modules.discord.util.commands.message
import dev.redicloud.core.modules.discord.util.commands.messages
import dev.redicloud.core.modules.discord.util.interactions.button
import dev.redicloud.core.modules.discord.util.message.useDefaultDesign
import dev.redicloud.core.modules.discord.util.snowflake
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TicketPart : DiscordModulePart() {

    override val name = "Ticket"
    val tickets: ExternalRMap<UUID, Ticket> =
        ICoreAPI.INSTANCE.redisConnection.getRedissonClient().getExternalMap("module-data:discord:ticket:tickets")
    lateinit var config: TicketConfig

    override suspend fun init() {
        if (ICoreAPI.INSTANCE.configManager.exists("discord:modules:ticket")) {
            config = ICoreAPI.INSTANCE.configManager.getConfig("discord:modules:ticket", TicketConfig::class.java)
        } else {
            config = TicketConfig()
            ICoreAPI.INSTANCE.configManager.createConfig(config)
        }
        loadConfigCommand()
        startScheduler()
    }

    private fun startScheduler() {
        dev.redicloud.core.modules.discord.ioScope.launch {
            repeat(100_000) {
                tickets.values.forEach { ticket ->
                    val channel = dev.redicloud.core.modules.discord.mainGuild.getChannelOrNull(ticket.channelId.snowflake)
                    if (!ticket.isOpen()) {
                        if (ticket.getArchiveTime() + config.deleteAfterArchive <= System.currentTimeMillis()) {
                            ticket.stateHistory[System.currentTimeMillis()] =
                                TicketState.DELETED to dev.redicloud.core.modules.discord.kord.selfId.value.toLong()
                            ticket.update()
                            var content = "Not available anymore, because the ticket was deleted manually!"
                            if (channel != null) {
                                content =
                                    "Ticket ID: ${ticket.id}\n" + "Ticket Owner: <@${ticket.creatorId}>\n" + "Ticket Channel: <#${ticket.channelId}>\n" + "Ticket State History: \n" + "- ${
                                        ticket.stateHistory.map { (state, i) ->
                                            "$state by ${
                                                dev.redicloud.core.modules.discord.kord.getUser(i.second.snowflake).let {
                                                    return@let if (it != null) it.mention + "#" + it.discriminator else "Unknown User (${i.second})"
                                                }
                                            }"
                                        }
                                    }\n\n\n"

                                channel.asChannelOf<TextChannel>().messages.collect() {
                                    val userName = it.author?.let { user ->
                                        return@let user.mention + "#" + user.discriminator
                                    } ?: "Unknown User"
                                    content += "${it.author?.username}#${it.author?.discriminator} (${
                                        SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(it.timestamp.epochSeconds * 1000))
                                    }): ${it.content}\n"
                                    it.embeds.forEach {
                                        content += "Embed: ${it.title} - ${it.description} - ${
                                            it.fields.map { it.name + ": " + it.value }.joinToString { "; " }
                                        }\n"
                                    }
                                }
                                channel.delete()
                            }

                            val archiveConversionUrl = content.haste(System.getenv("EXCEPTION_HASTE_URL"))

                            val archiveChannel = dev.redicloud.core.modules.discord.mainGuild.getChannelOrNull(config.archiveChannel.snowflake)
                            if (archiveChannel != null) {
                                archiveChannel.asChannelOf<TextChannel>().createMessage {
                                    embed {
                                        title = "Archive | $networkName"
                                        description = "Ticket ID: ${ticket.id}\n" +
                                                "Ticket Owner: <@${ticket.creatorId}>\n" +
                                                "Ticket Channel: <#${ticket.channelId}>\n" +
                                                "Ticket State History: \n" +
                                                "- ${
                                                    ticket.stateHistory.map { (state, i) ->
                                                        "$state by ${
                                                            dev.redicloud.core.modules.discord.kord.getUser(i.second.snowflake).let {
                                                                return@let if (it != null) it.mention + "#" + it.discriminator else "Unknown User (${i.second})"
                                                            }
                                                        }"
                                                    }
                                        }\n" +
                                                "Archive-URL: $archiveConversionUrl"
                                    }
                                }
                            }
                        }
                        return@forEach
                    }
                    if (channel == null) {
                        ticket.stateHistory[System.currentTimeMillis()] =
                            TicketState.DELETED to dev.redicloud.core.modules.discord.kord.selfId.value.toLong()
                        ticket.update()
                        return@forEach
                    }
                    val messageChannel = channel.asChannelOf<GuildMessageChannel>()
                    if (ticket.lastUserMessage + config.tagAfterNoResponse < System.currentTimeMillis() && !ticket.inactivityNotify) {
                        messageChannel.createMessage(config.inactivityNotifyMessage)
                        ticket.inactivityNotify = true
                        ticket.update()
                        return@forEach
                    }
                    if (ticket.lastUserMessage + config.closeAfterNoResponse < System.currentTimeMillis() && ticket.inactivityNotify) {
                        closeTicket(dev.redicloud.core.modules.discord.mainGuild.getMember(dev.redicloud.core.modules.discord.kord.selfId), null, ticket.channelId.snowflake)
                        return@forEach
                    }
                }

                delay(30.seconds.inWholeMilliseconds)
            }
        }
    }

    private suspend fun createTicket(user: User, interaction: ActionInteractionBehavior) {
        val count = tickets.count { it.value.creatorId == user.id.value.toLong() && it.value.isOpen() }
        if (count >= config.maxOpenTicketsPerUser) {
            interaction.respondEphemeral {
                embed {
                    title = "Error | $networkName"
                    description = "You have reached the maximum amount of open tickets!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(user)
                }
            }
            return
        }

        val ticket = Ticket()
        ticket.stateHistory[System.currentTimeMillis()] = TicketState.OPENED to user.id.value.toLong()
        ticket.lastUserMessage = System.currentTimeMillis()
        ticket.users.add(user.id.value.toLong())

        val channel =
            dev.redicloud.core.modules.discord.mainGuild.createTextChannel("${config.channelPrefix}${config.channelIdentifierType.parse(user)}") {
                parentId = config.category.snowflake
                permissionOverwrites += Overwrite(
                    dev.redicloud.core.modules.discord.mainGuild.everyoneRole.id,
                    OverwriteType.Role,
                    allow = Permissions(),
                    deny = Permissions(Permission.ViewChannel, Permission.SendMessages)
                )
                permissionOverwrites += Overwrite(
                    user.id, OverwriteType.Member, deny = Permissions(), allow = Permissions(Permission.ViewChannel)
                )
                permissionOverwrites += Overwrite(
                    config.supportRole.snowflake,
                    OverwriteType.Role,
                    deny = Permissions(),
                    allow = Permissions(Permission.ViewChannel)
                )
            }.also {
                ticket.channelId = it.id.value.toLong()
                tickets[ticket.id] = ticket

                it.createMessage(
                    config.ticketWelcomeMessage, user, mutableMapOf(
                        "user" to user.mention,
                        "close_emoji" to config.closeEmoji.mention(),
                        "confirm_emoji" to config.confirmEmoji.mention()
                    )
                ) {
                    {
                        button(ButtonStyle.Success, "TICKET_RULES_CONFIRM") {
                            emoji = config.confirmEmoji.partialEmoji()
                        }
                        button(ButtonStyle.Danger, "TICKET_CLOSE") {
                            emoji = config.closeEmoji.partialEmoji()
                        }
                    }
                }

                interaction.respondEphemeral {
                    embed {
                        title = "Ticket | $networkName"
                        description =
                            "Your ticket has been created successfully and can be found here: ${it.asChannelOf<TextChannel>().mention}"
                        useDefaultDesign(user)
                    }
                }
            }
    }

    private suspend fun closeTicket(
        user: Member?, interaction: ActionInteractionBehavior?, channelId: Snowflake? = null
    ) {
        if (channelId == null && interaction == null) throw IllegalArgumentException("Either interaction or channelId must be provided!")
        val entry = tickets.entries.find {
            if (channelId != null) {
                it.value.channelId == channelId.value.toLong()
            } else {
                it.value.channelId == interaction!!.channelId.value.toLong()
            }
        }
        val ticket = entry?.value
        if (ticket == null) { // If interaction is null, it´s called from the scheduler and the ticket existing
            if (user == null) return
            interaction!!.respondEphemeral {
                embed {
                    title = "Error | $networkName"
                    description = "This ticket does not exist! Please contact a staff member!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(user)
                }
            }
            return
        }
        if (user != null) {
            if (user.id.value.toLong() != ticket.creatorId && !user.roleIds.contains(config.supportRole.snowflake) && !user.isBot) {
                interaction!!.respondEphemeral {
                    embed {
                        title = "Error | $networkName"
                        description = "You are not allowed to close this ticket!"
                        color = Color(250, 0, 0)
                        useDefaultDesign(user)
                    }
                }
                return
            }
        }
        if (!ticket.isOpen()) { // If interaction is null, it´s called from the scheduler and the ticket existing
            if (interaction == null) return
            interaction.respondEphemeral {
                embed {
                    title = "Error | $networkName"
                    description = "This ticket is already closed!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(user)
                }
            }
            return
        }
        val channel = dev.redicloud.core.modules.discord.mainGuild.getChannel(ticket.channelId.snowflake).asChannelOf<TextChannel>()
        if (interaction != null) {
            interaction.respondEphemeral(config.closeConfirmMessage, user) {
                {
                    button(ButtonStyle.Success, "TICKET_CLOSE_CONFIRM") {
                        emoji = config.confirmEmoji.partialEmoji()
                    }
                }
            }
        } else if (user == null) {
            channel.createMessage {
                embed {
                    title = "Ticket | $networkName"
                    description = "This ticket has been closed due the user leaving the server!"
                    useDefaultDesign(dev.redicloud.core.modules.discord.kord.getSelf())
                }
            }
            archiveTicket(ticket, dev.redicloud.core.modules.discord.kord.getSelf())
        } else {
            channel.createMessage(config.inactivityCloseMessage, user)
            archiveTicket(ticket, dev.redicloud.core.modules.discord.kord.getSelf())
        }
    }

    private fun archiveTicket(ticket: Ticket, user: User) {
        dev.redicloud.core.modules.discord.ioScope.launch {
            ticket.stateHistory[System.currentTimeMillis()] = TicketState.CLOSED to user.id.value.toLong()
            ticket.update()

            delay(20.seconds)

            val channel = dev.redicloud.core.modules.discord.mainGuild.getChannel(ticket.channelId.snowflake).asChannelOf<TextChannel>()
            channel.edit {
                parentId = config.archiveCategory.snowflake
                if (permissionOverwrites == null) permissionOverwrites = mutableSetOf()
                ticket.users.forEach {
                    permissionOverwrites!! += Overwrite(
                        it.snowflake, OverwriteType.Member, deny = Permissions(), allow = Permissions()
                    )
                }
                permissionOverwrites!! += Overwrite(
                    config.supportRole.snowflake, OverwriteType.Role, deny = Permissions(), allow = Permissions()
                )
                permissionOverwrites!! += Overwrite(
                    config.archiveViewRole.snowflake,
                    OverwriteType.Role,
                    deny = Permissions(),
                    allow = Permissions(Permission.ViewChannel)
                )
                permissionOverwrites!! += Overwrite(
                    dev.redicloud.core.modules.discord.mainGuild.everyoneRole.id,
                    OverwriteType.Role,
                    deny = Permissions(Permission.ViewChannel),
                    allow = Permissions()
                )
            }
        }
    }

    private val buttonListener = dev.redicloud.core.modules.discord.kord.on<GuildButtonInteractionCreateEvent> {
        when (interaction.componentId) {
            "TICKET_CLOSE" -> {
                closeTicket(interaction.user, interaction)
            }

            "TICKET_OPEN" -> {
                createTicket(interaction.user, interaction)
            }

            "TICKET_RULES_CONFIRM" -> {
                val ticket = getTicket(interaction.channel.id)
                if (ticket == null) {
                    interaction.respondEphemeral {
                        embed {
                            title = "Error | $networkName"
                            description = "This ticket does not exist! Please contact a staff member!"
                            color = Color(250, 0, 0)
                            useDefaultDesign(interaction.user)
                        }
                    }
                    return@on
                }
                if (!ticket.isOpen()) {
                    interaction.respondEphemeral {
                        embed {
                            title = "Error | $networkName"
                            description = "This ticket is already closed!"
                            color = Color(250, 0, 0)
                            useDefaultDesign(interaction.user)
                        }
                    }
                    return@on
                }
                interaction.respondPublic(
                    config.confirmMessage, interaction.user
                )
                interaction.channel.asChannelOf<TextChannel>().editRolePermission(dev.redicloud.core.modules.discord.mainGuild.everyoneRole.id) {
                    allowed = Permissions(Permission.SendMessages)
                    denied = Permissions(Permission.ViewChannel)
                }
                interaction.channel.createMessage(dev.redicloud.core.modules.discord.mainGuild.getRole(config.supportRole.snowflake).mention)
                    .also { it.delete() }
            }

            "TICKET_CLOSE_CONFIRM" -> {
                val ticket = getTicket(interaction.channel.id)
                if (ticket == null) {
                    interaction.respondEphemeral {
                        embed {
                            title = "Error | $networkName"
                            description = "This ticket does not exist! Please contact a staff member!"
                            color = Color(250, 0, 0)
                            useDefaultDesign(interaction.user)
                        }
                    }
                    return@on
                }
                if (interaction.user.id.value.toLong() != ticket.creatorId && !interaction.user.roleIds.contains(config.supportRole.snowflake)) {
                    interaction.respondEphemeral {
                        embed {
                            title = "Error | $networkName"
                            description = "You are not allowed to close this ticket!"
                            color = Color(250, 0, 0)
                            useDefaultDesign(interaction.user)
                        }
                    }
                    return@on
                }
                if (!ticket.isOpen()) {
                    interaction.respondEphemeral {
                        embed {
                            title = "Error | $networkName"
                            description = "This ticket is already closed!"
                            color = Color(250, 0, 0)
                            useDefaultDesign(interaction.user)
                        }
                    }
                    return@on
                }
                interaction.respondPublic {
                    embed {
                        title = "Ticket | $networkName"
                        description = "This ticket has been closed by ${interaction.user.mention}"
                        color = Color(0, 250, 0)
                        useDefaultDesign(interaction.user)
                    }
                }
                archiveTicket(ticket, interaction.user)
            }
        }
    }

    private val messageListener = dev.redicloud.core.modules.discord.kord.on<MessageCreateEvent> {
        val ticket = getTicket(message.channelId)
        if (ticket == null) return@on
        if (!ticket.isOpen()) return@on
        val author = message.author ?: return@on
        if (!ticket.users.contains(author.id.value.toLong())) return@on
        ticket.lastUserMessage = System.currentTimeMillis()
        ticket.inactivityNotify = false
        ticket.update()
    }

    private val userLeaveListener = dev.redicloud.core.modules.discord.kord.on<MemberLeaveEvent> {
        val tickets = tickets.values.filter { it.users.contains(user.id.value.toLong()) }
        tickets.forEach {
            if (it.creatorId == user.id.value.toLong()) {
                closeTicket(null, null, it.channelId.snowflake)
            } else {
                it.users.remove(user.id.value.toLong())
                it.update()
            }
        }
    }

    fun getTicket(channelId: Snowflake): Ticket? = tickets.values.find { it.channelId == channelId.value.toLong() }

    private fun loadConfigCommand() {
        dev.redicloud.core.modules.discord.DiscordModuleMain.INSTANCE.configCommands.forEach {
            it.value.apply {
                group("ticket", "Configure the ticket module") {
                    subCommand("create", "Create the open message button") {
                        channel("channel", "The channel to create the message button in") {
                            required = false
                        }
                        perform(this@group, this) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val channelBehavior = interaction.command.channels["channel"] ?: interaction.channel
                                channelBehavior.asChannelOf<TextChannel>().createMessage {
                                    embed {
                                        title = "Ticket | $networkName"
                                        description = "Click the button below to open a ticket"
                                        useDefaultDesign(interaction.user)
                                    }
                                    actionRow {
                                        button(ButtonStyle.Success, "TICKET_OPEN") {
                                            emoji = config.confirmEmoji.partialEmoji()
                                        }
                                    }
                                }.also {
                                    config.openMessages.add(it.id.value.toLong())
                                    ICoreAPI.INSTANCE.configManager.saveConfig(config)
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Info | $networkName"
                                            description =
                                                "The ticket open message has been created in ${channelBehavior.mention}"
                                            useDefaultDesign(interaction.user)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    subCommand("inactivity_notify_message", "Set the inactivity notify message") {
                        string("message", "The message to send") {
                            required = false
                        }
                        perform(this@group, this) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val message = interaction.command.messages["message"]
                                if (message == null) {
                                    if (interaction.command.strings["message"] == null) {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Info | $networkName"
                                                description = "The current inactivity notify message is:"
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                        interaction.respondEphemeral(config.inactivityNotifyMessage, interaction.user)
                                    } else {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | $networkName"
                                                description = "The message is invalid! Please check your json syntax!"
                                                color = Color(250, 0, 0)
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                    }
                                    return@launch
                                }
                                config.inactivityNotifyMessage = message
                                ICoreAPI.INSTANCE.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The inactivity notify message has been set to:"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral(message, interaction.user)
                            }
                        }
                    }
                    subCommand("inactivity_close_message", "Set the inactivity close message") {
                        string("message", "The message to send") {
                            required = false
                        }
                        perform(this@group, this) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val message = interaction.command.messages["message"]
                                if (message == null) {
                                    if (interaction.command.strings["message"] == null) {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Info | $networkName"
                                                description = "The current inactivity close message is:"
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                        interaction.respondEphemeral(config.inactivityCloseMessage, interaction.user)
                                    } else {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | $networkName"
                                                description = "The message is invalid! Please check your json syntax!"
                                                color = Color(250, 0, 0)
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                    }
                                    return@launch
                                }
                                config.inactivityCloseMessage = message
                                ICoreAPI.INSTANCE.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The inactivity close message has been set to:"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral(message, interaction.user)
                            }
                        }
                    }
                    subCommand("close_confirm_message", "Set the close confirm message") {
                        string("message", "The message to send") {
                            required = false
                        }
                        perform(this@group, this) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val message = interaction.command.messages["message"]
                                if (message == null) {
                                    if (interaction.command.strings["message"] == null) {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Info | $networkName"
                                                description = "The current close confirm message is:"
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                        interaction.respondEphemeral(config.closeConfirmMessage, interaction.user)
                                    } else {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | $networkName"
                                                description = "The message is invalid! Please check your json syntax!"
                                                color = Color(250, 0, 0)
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                    }
                                    return@launch
                                }
                                config.closeConfirmMessage = message
                                ICoreAPI.INSTANCE.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The close confirm message has been set to:"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral(message, interaction.user)
                            }
                        }
                    }
                    subCommand("welcome_message", "Set the welcome message") {
                        message("message", "The message to send") {
                            required = false
                        }
                        perform(this@group, this) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val message = interaction.command.messages["message"]
                                if (message == null) {
                                    if (interaction.command.strings["message"] == null) {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Info | $networkName"
                                                description = "The current welcome message is:"
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                        interaction.respondEphemeral(config.ticketWelcomeMessage, interaction.user)
                                    } else {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | $networkName"
                                                description = "The message is invalid! Please check your json syntax!"
                                                color = Color(250, 0, 0)
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                    }
                                    return@launch
                                }
                                config.ticketWelcomeMessage = message
                                ICoreAPI.INSTANCE.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The welcome message has been set to:"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral(message, interaction.user)
                            }
                        }
                    }
                    subCommand("confirm_essage", "Set the ticket rule config message") {
                        message("message", "The message to send") {
                            required = false
                        }
                        perform(this@group, this) {
                            dev.redicloud.core.modules.discord.ioScope.launch {
                                val message = interaction.command.messages["message"]
                                if (message == null) {
                                    if (interaction.command.strings["message"] == null) {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Info | $networkName"
                                                description = "The current ticket rule config message is:"
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                        interaction.respondEphemeral(config.confirmMessage, interaction.user)
                                    } else {
                                        interaction.respondEphemeral {
                                            embed {
                                                title = "Error | $networkName"
                                                description = "The message is invalid! Please check your json syntax!"
                                                color = Color(250, 0, 0)
                                                useDefaultDesign(interaction.user)
                                            }
                                        }
                                    }
                                    return@launch
                                }
                                config.confirmMessage = message
                                ICoreAPI.INSTANCE.configManager.saveConfig(config)
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The ticket rule config message has been set to:"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                interaction.respondEphemeral(message, interaction.user)
                            }
                        }
                    }
                }
            }
        }
    }

    override val commands: List<CommandBuilder> =
        mutableListOf(inputCommand("ticket", dev.redicloud.core.modules.discord.mainGuild.id, "Ticket commands") {
            subCommand("create", "Create a ticket") {
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        createTicket(interaction.user, interaction)
                    }
                }
            }
            subCommand("open", "Create a ticket") {
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        createTicket(interaction.user, interaction)
                    }
                }
            }
            subCommand("close", "Close a ticket") {
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        closeTicket(interaction.user, interaction)
                    }
                }
            }
            subCommand("add", "Add a user to a ticket") {
                user("user", "The user to add") {
                    required = true
                }
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        val ticket = getTicket(interaction.channel.id)
                        if (ticket == null) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | $networkName"
                                    description = "This ticket does not exist! Please contact a staff member!"
                                    color = Color(250, 0, 0)
                                    useDefaultDesign(interaction.user)
                                }
                            }
                            return@launch
                        }
                        if (interaction.user.id.value.toLong() != ticket.creatorId && !interaction.user.roleIds.contains(config.supportRole.snowflake)) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | $networkName"
                                    description = "You are not allowed to add users to this ticket!"
                                }
                            }
                            return@launch
                        }
                        val user = interaction.command.users["user"]!!
                        ticket.users.add(user.id.value.toLong())
                        ticket.update()
                        interaction.channel.asChannelOf<TextChannel>().edit {
                            permissionOverwrites!! += Overwrite(
                                user.id,
                                OverwriteType.Member,
                                allow = Permissions(Permission.ViewChannel),
                                deny = Permissions()
                            )
                        }
                        interaction.respondPublic {
                            embed {
                                title = "Info | $networkName"
                                description = "The user ${user.mention} has been added to this ticket!"
                                useDefaultDesign(interaction.user)
                            }
                        }
                    }
                }
            }
            subCommand("remove", "Remove a user from a ticket") {
                user("user", "The user to add") {
                    required = true
                }
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        val ticket = getTicket(interaction.channel.id)
                        if (ticket == null) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | $networkName"
                                    description = "This ticket does not exist! Please contact a staff member!"
                                    color = Color(250, 0, 0)
                                    useDefaultDesign(interaction.user)
                                }
                            }
                            return@launch
                        }
                        if (interaction.user.id.value.toLong() != ticket.creatorId && !interaction.user.roleIds.contains(config.supportRole.snowflake)) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | $networkName"
                                    description = "You are not allowed to remove users from this ticket!"
                                }
                            }
                            return@launch
                        }
                        val user = interaction.command.users["user"]!!
                        ticket.users.remove(user.id.value.toLong())
                        ticket.update()
                        interaction.channel.asChannelOf<TextChannel>().edit {
                            permissionOverwrites!! += Overwrite(
                                user.id,
                                OverwriteType.Member,
                                allow = Permissions(),
                                deny = Permissions()
                            )
                        }
                        interaction.respondPublic {
                            embed {
                                title = "Info | $networkName"
                                description = "The user ${user.mention} has been removed from this ticket!"
                                useDefaultDesign(interaction.user)
                            }
                        }
                    }
                }
            }
            subCommand("info", "Info about the current ticket") {
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        val ticket = getTicket(interaction.channel.id)
                        if (ticket == null) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | $networkName"
                                    description =
                                        "This ticket does not exist! Please contact a staff member if you think this is an error!"
                                    color = Color(250, 0, 0)
                                    useDefaultDesign(interaction.user)
                                }
                            }
                            return@launch
                        }
                        val users = ticket.users.mapNotNull {
                            if (it == ticket.creatorId) return@mapNotNull null
                            return@mapNotNull kord.getUser(it.snowflake)
                        }
                        val creator = kord.getUser(ticket.creatorId.snowflake)
                        interaction.respondEphemeral {
                            embed {
                                title = "Ticket | $networkName"
                                description = ":round_pushpin: Ticket ID: ${
                                    ticket.id
                                }\n" + ":bellhop: Creator: <@${
                                    creator?.id?.value ?: ticket.creatorId
                                }>\n" + ":gear: Users: ${
                                    users.joinToString(", ") { "<@${it.mention}>" }
                                }\n" + ":envelope_with_arrow: Created at: ${
                                    ticket.stateHistory.firstNotNullOf {
                                        SimpleDateFormat("HH:mm dd.MM.yyyy").format(ticket.stateHistory.firstNotNullOfOrNull { it.key })
                                    }
                                }\n" + ":calling: Last user message: ${
                                    ticket.stateHistory.firstNotNullOf {
                                        SimpleDateFormat("HH:mm dd.MM.yyyy").format(ticket.lastUserMessage)
                                    }
                                }\n" + ":hourglass_flowing_sand: Inactivity notify: ${
                                    ticket.inactivityNotify
                                }\n" + ":calendar_spiral: History: \n${
                                    ticket.stateHistory.map {
                                        " • ${SimpleDateFormat("HH:mm dd.MM.yyyy").format(it.key)}: ${
                                            when (it.value.first) {
                                                TicketState.OPENED -> "Open"
                                                TicketState.CLOSED -> "Closed (Archived)"
                                                TicketState.DELETED -> "Closed (Deleted)"
                                                TicketState.REOPENED -> "Reopened"
                                            }
                                        }"
                                    }.joinToString("\n")
                                }"
                                useDefaultDesign(interaction.user)
                            }
                        }
                    }
                }
            }
        }, inputCommand("ticket-control", dev.redicloud.core.modules.discord.mainGuild.id, "Ticket control commands") {
            permissions += Permission.All
            group("dev", "Development commands") {
                subCommand("lastusermessage", "Set the time of the last user message manually") {
                    string("id", "The id of the ticket") {
                        required = true
                    }
                    string("time", "The time in milliseconds") {
                        required = false
                    }
                    perform(this@group, this) {
                        dev.redicloud.core.modules.discord.ioScope.launch {
                            val id = try {
                                UUID.fromString(interaction.command.strings["id"]!!)
                            } catch (e: Exception) {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | $networkName"
                                        description = "The id is not valid!"
                                        color = Color(250, 0, 0)
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                return@launch
                            }
                            val ticket = tickets[id] ?: run {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | $networkName"
                                        description = "The ticket does not exist!"
                                        color = Color(250, 0, 0)
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                return@launch
                            }
                            val time = try {
                                interaction.command.strings["time"]?.toLong() ?: ticket.lastUserMessage
                            } catch (e: Exception) {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | $networkName"
                                        description = "The time is not valid!"
                                        color = Color(250, 0, 0)
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                return@launch
                            }
                            if (interaction.command.strings["time"] != null) {
                                ticket.lastUserMessage = time
                                ticket.update()
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The last user message has been set to ${
                                            SimpleDateFormat("HH:mm dd.MM.yyyy").format(time)
                                        }"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            } else {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The last user message time has been set to ${
                                            SimpleDateFormat("HH:mm dd.MM.yyyy").format(time)
                                        }"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                }
                subCommand("inacitvitynotified", "Set the inactivity notified state manually") {
                    string("id", "The id of the ticket") {
                        required = true
                    }
                    boolean("state", "The state of the inactivity notified") {
                        required = false
                    }
                    perform(this@group, this) {
                        dev.redicloud.core.modules.discord.ioScope.launch {
                            val id = try {
                                UUID.fromString(interaction.command.strings["id"]!!)
                            } catch (e: Exception) {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | $networkName"
                                        description = "The id is not valid!"
                                        color = Color(250, 0, 0)
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                return@launch
                            }
                            val ticket = tickets[id] ?: run {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | $networkName"
                                        description = "The ticket does not exist!"
                                        color = Color(250, 0, 0)
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                                return@launch
                            }
                            val state = interaction.command.booleans["state"] ?: ticket.inactivityNotify
                            if (interaction.command.booleans["state"] != null) {
                                ticket.inactivityNotify = state
                                ticket.update()
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The inactivity notified state has been set to $state"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            } else {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | $networkName"
                                        description = "The inactivity notified state is $state"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            subCommand("archive-view", "Toggle archive-view") {
                perform(null, this) {
                    dev.redicloud.core.modules.discord.ioScope.launch {
                        val role = dev.redicloud.core.modules.discord.mainGuild.getRoleOrNull(config.archiveViewRole.snowflake)
                        if (role == null) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | $networkName"
                                    description =
                                        "The archive-view role does not exist! Please contact an administrator!"
                                    color = Color(250, 0, 0)
                                    useDefaultDesign(interaction.user)
                                }
                            }
                            return@launch
                        }
                        val member = dev.redicloud.core.modules.discord.mainGuild.getMember(interaction.user.id)
                        if (member.roleIds.contains(role.id)) {
                            member.removeRole(role.id)
                            interaction.respondEphemeral {
                                embed {
                                    title = "Info | $networkName"
                                    description = "You can´t view archived tickets anymore!"
                                    useDefaultDesign(interaction.user)
                                }
                            }
                        } else {
                            member.addRole(role.id)
                            interaction.respondEphemeral {
                                embed {
                                    title = "Info | $networkName"
                                    description = "You can now view archived tickets!"
                                    useDefaultDesign(interaction.user)
                                }
                            }
                        }
                    }
                }
            }
        })

}