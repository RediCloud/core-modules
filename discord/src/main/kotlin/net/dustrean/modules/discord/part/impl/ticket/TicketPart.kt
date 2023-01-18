package net.dustrean.modules.discord.part.impl.ticket

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.*
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.launch
import net.dustrean.api.ICoreAPI
import net.dustrean.api.utils.extension.ExternalRMap
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
import net.dustrean.modules.discord.util.interactions.button
import net.dustrean.modules.discord.util.message.useDefaultDesign
import net.dustrean.modules.discord.util.snowflake
import java.util.UUID

object TicketPart : DiscordModulePart() {

    override val name = "Ticket"
    override val commands: List<CommandBuilder> = mutableListOf()
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
        ticket.lastCreatorMessage = System.currentTimeMillis()

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
                        title = "Ticket | DustreanNET"
                        description =
                            "Your ticket has been created successfully and can be found here: ${it.asChannelOf<TextChannel>().mention}"
                        useDefaultDesign(user)
                    }
                }
            }
    }

    private suspend fun closeTicket(user: Member, interaction: ActionInteractionBehavior) {
        val entry = tickets.entries.find { it.value.channelId == interaction.channel.id.value.toLong() }
        val ticket = entry?.value
        if (ticket == null) {
            interaction.respondEphemeral {
                embed {
                    title = "Error | DustreanNET"
                    description = "This ticket does not exist! Please contact a staff member!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(user)
                }
            }
            return
        }
        if (user.id.value.toLong() != ticket.creatorId && !user.roleIds.contains(config.supportRole.snowflake)) {
            interaction.respondEphemeral {
                embed {
                    title = "Error | DustreanNET"
                    description = "You are not allowed to close this ticket!"
                    color = Color(250, 0, 0)
                    useDefaultDesign(user)
                }
            }
            return
        }
        if (!ticket.isOpen()) {
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
        interaction.respondEphemeral(config.closeConfirmMessage, user) {
            {
                button(ButtonStyle.Success, "TICKET_CLOSE_CONFIRM") {
                    emoji = config.confirmEmoji.partialEmoji()
                }
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
                val entry = tickets.entries.find { it.value.channelId == interaction.channel.id.value.toLong() }
                val ticket = entry?.value
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
                interaction.channel.createMessage(mainGuild.getRole(config.supportRole.snowflake).mention).also { it.delete() }
            }
            "TICKET_CLOSE_CONFIRM" -> {
                val entry = tickets.entries.find { it.value.channelId == interaction.channel.id.value.toLong() }
                val ticket = entry?.value
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
                ticket.stateHistory[System.currentTimeMillis()] = TicketState.CLOSED to interaction.user.id.value.toLong()
                ticket.update()
                interaction.respondPublic {
                    embed {
                        title = "Ticket | DustreanNET"
                        description = "This ticket has been closed by ${interaction.user.mention}"
                        color = Color(0, 250, 0)
                        useDefaultDesign(interaction.user)
                    }
                }
            }
        }
    }

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

}