package net.dustrean.modules.discord.part.impl.ticket

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
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dustrean.api.ICoreAPI
import net.dustrean.api.utils.extension.ExternalRMap
import net.dustrean.api.utils.extension.JsonObjectData
import net.dustrean.api.utils.extension.getExternalMap
import net.dustrean.modules.discord.DiscordModuleMain
import net.dustrean.modules.discord.data.createMessage
import net.dustrean.modules.discord.data.respondEphemeral
import net.dustrean.modules.discord.data.respondPublic
import net.dustrean.modules.discord.ioScope
import net.dustrean.modules.discord.kord
import net.dustrean.modules.discord.mainGuild
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder
import net.dustrean.modules.discord.util.commands.inputCommand
import net.dustrean.modules.discord.util.interactions.button
import net.dustrean.modules.discord.util.message.useDefaultDesign
import net.dustrean.modules.discord.util.snowflake
import org.redisson.api.RMap
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TicketPart : DiscordModulePart() {

    override val name = "Ticket"
    val tickets: ExternalRMap<UUID, Ticket> =
        ICoreAPI.INSTANCE.getRedisConnection().getRedissonClient().getExternalMap("module-data:discord:ticket:tickets")
    lateinit var config: TicketConfig

    override suspend fun init() {
        if (ICoreAPI.INSTANCE.getConfigManager().exists("discord:modules:ticket")) {
            config = ICoreAPI.INSTANCE.getConfigManager().getConfig("discord:modules:ticket", TicketConfig::class.java)
        } else {
            config = TicketConfig()
            ICoreAPI.INSTANCE.getConfigManager().createConfig(config)
        }
        loadConfigCommand()
        startScheduler()
    }

    private fun startScheduler() {
        ioScope.launch {
            repeat(100_000) {
                tickets.values.forEach {
                    if (!it.isOpen()) {
                        if (it.getArchiveTime() + config.deleteAfterArchive <= System.currentTimeMillis()) {
                            //TODO: save conversion messages
                            it.stateHistory[System.currentTimeMillis()] =
                                TicketState.DELETED to kord.selfId.value.toLong()
                            it.update()
                            val channel = mainGuild.getChannelOrNull(it.channelId.snowflake)
                            if (channel != null) {
                                channel.delete()
                            }
                        }
                        return@forEach
                    }
                    val channel = mainGuild.getChannelOrNull(it.channelId.snowflake)
                    if (channel == null) {
                        it.stateHistory[System.currentTimeMillis()] = TicketState.DELETED to kord.selfId.value.toLong()
                        it.update()
                        return@forEach
                    }
                    val messageChannel = channel.asChannelOf<GuildMessageChannel>()
                    if (it.lastUserMessage + config.tagAfterNoResponse < System.currentTimeMillis() && !it.inactivityNotify) {
                        messageChannel.createMessage(config.inactivityNotifyMessages)
                        it.inactivityNotify = true
                        it.update()
                        return@forEach
                    }
                    if (it.lastUserMessage + config.closeAfterNoResponse < System.currentTimeMillis() && it.inactivityNotify) {
                        closeTicket(mainGuild.getMember(kord.selfId), null, it.channelId.snowflake)
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
                    title = "Error | DustreanNET"
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
            mainGuild.createTextChannel("${config.channelPrefix}${config.channelIdentifierType.parse(user)}") {
                parentId = config.category.snowflake
                permissionOverwrites += Overwrite(
                    mainGuild.everyoneRole.id,
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
                    config.ticketWelcomeMessages, user, mutableMapOf(
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
                        title = "Ticket | DustreanNET"
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
                    title = "Error | DustreanNET"
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
                        title = "Error | DustreanNET"
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
                    title = "Error | DustreanNET"
                    description = "This ticket is already closed!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(user)
                }
            }
            return
        }
        val channel = mainGuild.getChannel(ticket.channelId.snowflake).asChannelOf<TextChannel>()
        if (interaction != null) {
            interaction.respondEphemeral(config.closeConfirmMessages, user) {
                {
                    button(ButtonStyle.Success, "TICKET_CLOSE_CONFIRM") {
                        emoji = config.confirmEmoji.partialEmoji()
                    }
                }
            }
        } else if (user == null) {
            channel.createMessage {
                embed {
                    title = "Ticket | DustreanNET"
                    description = "This ticket has been closed due the user leaving the server!"
                    useDefaultDesign(kord.getSelf())
                }
            }
            archiveTicket(ticket, kord.getSelf())
        } else {
            channel.createMessage(config.inactivityCloseMessages, user)
            archiveTicket(ticket, kord.getSelf())
        }
    }

    private fun archiveTicket(ticket: Ticket, user: User) {
        ioScope.launch {
            ticket.stateHistory[System.currentTimeMillis()] = TicketState.CLOSED to user.id.value.toLong()
            ticket.update()

            delay(20.seconds)

            val channel = mainGuild.getChannel(ticket.channelId.snowflake).asChannelOf<TextChannel>()
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
                    config.archiveViewRole.snowflake, OverwriteType.Role, deny = Permissions(), allow = Permissions(Permission.ViewChannel)
                )
                permissionOverwrites!! += Overwrite(
                    mainGuild.everyoneRole.id, OverwriteType.Role, deny = Permissions(Permission.ViewChannel), allow = Permissions()
                )
            }
        }
    }

    private val buttonListener = kord.on<GuildButtonInteractionCreateEvent> {
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
                            title = "Error | DustreanNET"
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
                            title = "Error | DustreanNET"
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
                interaction.channel.asChannelOf<TextChannel>().editRolePermission(mainGuild.everyoneRole.id) {
                    allowed = Permissions(Permission.SendMessages)
                    denied = Permissions(Permission.ViewChannel)
                }
                interaction.channel.createMessage(mainGuild.getRole(config.supportRole.snowflake).mention)
                    .also { it.delete() }
            }

            "TICKET_CLOSE_CONFIRM" -> {
                val ticket = getTicket(interaction.channel.id)
                if (ticket == null) {
                    interaction.respondEphemeral {
                        embed {
                            title = "Error | DustreanNET"
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
                            title = "Error | DustreanNET"
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
                            title = "Error | DustreanNET"
                            description = "This ticket is already closed!"
                            color = Color(250, 0, 0)
                            useDefaultDesign(interaction.user)
                        }
                    }
                    return@on
                }
                interaction.respondPublic {
                    embed {
                        title = "Ticket | DustreanNET"
                        description = "This ticket has been closed by ${interaction.user.mention}"
                        color = Color(0, 250, 0)
                        useDefaultDesign(interaction.user)
                    }
                }
                archiveTicket(ticket, interaction.user)
            }
        }
    }

    private val messageListener = kord.on<MessageCreateEvent> {
        val ticket = getTicket(message.channelId)
        if (ticket == null) return@on
        if (!ticket.isOpen()) return@on
        val author = message.author ?: return@on
        if (!ticket.users.contains(author.id.value.toLong())) return@on
        ticket.lastUserMessage = System.currentTimeMillis()
        ticket.inactivityNotify = false
        ticket.update()
    }

    private val userLeaveListener = kord.on<MemberLeaveEvent> {
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
        DiscordModuleMain.CONFIG_COMMANDS.forEach {
            it.value.apply {
                group("ticket", "Configure the ticket module") {
                    subCommand("create", "Create the open message button") {
                        channel("channel", "The channel to create the message button in") {
                            required = false
                        }
                        perform(this@group, this) {
                            ioScope.launch {
                                val channelBehavior = interaction.command.channels["channel"] ?: interaction.channel
                                channelBehavior.asChannelOf<TextChannel>().createMessage {
                                    embed {
                                        title = "Ticket | DustreanNET"
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
                                    ICoreAPI.INSTANCE.getConfigManager().saveConfig(config)
                                    interaction.respondEphemeral {
                                        embed {
                                            title = "Info | DustreanNET"
                                            description =
                                                "The ticket open message has been created in ${channelBehavior.mention}"
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

    override val commands: List<CommandBuilder> =
        mutableListOf(inputCommand("ticket", mainGuild.id, "Ticket commands") {
            subCommand("create", "Create a ticket") {
                perform(null, this) {
                    ioScope.launch {
                        createTicket(interaction.user, interaction)
                    }
                }
            }
            subCommand("close", "Close a ticket") {
                perform(null, this) {
                    ioScope.launch {
                        closeTicket(interaction.user, interaction)
                    }
                }
            }
            subCommand("info", "Info about the current ticket") {
                perform(null, this) {
                    ioScope.launch {
                        val ticket = getTicket(interaction.channel.id)
                        if (ticket == null) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | DustreanNET"
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
                                title = "Ticket | DustreanNET"
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
        }, inputCommand("ticket-control", mainGuild.id, "Ticket control commands") {
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
                        ioScope.launch {
                            val id = try {
                                UUID.fromString(interaction.command.strings["id"]!!)
                            } catch (e: Exception) {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | DustreanNET"
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
                                        title = "Error | DustreanNET"
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
                                        title = "Error | DustreanNET"
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
                                        title = "Info | DustreanNET"
                                        description = "The last user message has been set to ${
                                            SimpleDateFormat("HH:mm dd.MM.yyyy").format(time)
                                        }"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            } else {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
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
                        ioScope.launch {
                            val id = try {
                                UUID.fromString(interaction.command.strings["id"]!!)
                            } catch (e: Exception) {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Error | DustreanNET"
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
                                        title = "Error | DustreanNET"
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
                                        title = "Info | DustreanNET"
                                        description = "The inactivity notified state has been set to $state"
                                        useDefaultDesign(interaction.user)
                                    }
                                }
                            } else {
                                interaction.respondEphemeral {
                                    embed {
                                        title = "Info | DustreanNET"
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
                    ioScope.launch {
                        val role = mainGuild.getRoleOrNull(config.archiveViewRole.snowflake)
                        if (role == null) {
                            interaction.respondEphemeral {
                                embed {
                                    title = "Error | DustreanNET"
                                    description =
                                        "The archive-view role does not exist! Please contact an administrator!"
                                    color = Color(250, 0, 0)
                                    useDefaultDesign(interaction.user)
                                }
                            }
                            return@launch
                        }
                        val member = mainGuild.getMember(interaction.user.id)
                        if (member.roleIds.contains(role.id)) {
                            member.removeRole(role.id)
                            interaction.respondEphemeral {
                                embed {
                                    title = "Info | DustreanNET"
                                    description = "You can´t view archived tickets anymore!"
                                    useDefaultDesign(interaction.user)
                                }
                            }
                        } else {
                            member.addRole(role.id)
                            interaction.respondEphemeral {
                                embed {
                                    title = "Info | DustreanNET"
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