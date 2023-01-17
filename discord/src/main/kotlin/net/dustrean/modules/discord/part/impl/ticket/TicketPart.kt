package net.dustrean.modules.discord.part.impl.ticket

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.launch
import net.dustrean.api.ICoreAPI
import net.dustrean.modules.discord.DiscordModuleMain
import net.dustrean.modules.discord.ioScope
import net.dustrean.modules.discord.kord
import net.dustrean.modules.discord.mainGuild
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder
import net.dustrean.modules.discord.util.interactions.InteractionCommandID
import net.dustrean.modules.discord.util.interactions.button
import net.dustrean.modules.discord.util.message.useDefaultFooter
import net.dustrean.modules.discord.util.snowflake
import org.redisson.api.RList

object TicketPart : DiscordModulePart() {

    override val name = "Ticket"
    override val commands: List<CommandBuilder> = mutableListOf()
    val tickets: RList<Ticket> =
        ICoreAPI.INSTANCE.getRedisConnection().getRedissonClient().getList("discord:module:ticket:list")
    lateinit var config: TicketConfig

    override suspend fun init() {
        if (ICoreAPI.INSTANCE.getConfigManager().exists("discord:module:ticket")) {
            config = ICoreAPI.INSTANCE.getConfigManager().getConfig("discord:module:ticket", TicketConfig::class.java)
        } else {
            config = TicketConfig()
            ICoreAPI.INSTANCE.getConfigManager().createConfig(config)
        }
        loadConfigCommand()
    }

    private val messageInteraction = kord.on<GuildButtonInteractionCreateEvent> {
        var button = false
        config.openMessages.forEach {
            interaction.message.let { message ->
                if (message.id.value.toLong() == it) {
                    button = true
                }
            }
        }
        if (!button) return@on

        val count = tickets.count { it.creatorId == interaction.user.id.value.toLong() && it.open }
        if (count >= config.maxOpenTicketsPerUser) {
            interaction.respondEphemeral {
                embed {
                    title = "Error | DustreanNET"
                    description = "You have reached the maximum amount of open tickets!"
                    color = Color(250, 0, 0)
                    useDefaultFooter(interaction.user)
                }
            }
            return@on
        }

        val ticket = Ticket()
        ticket.creatorId = interaction.user.id.value.toLong()
        ticket.open = true
        ticket.lastCreatorMessage = System.currentTimeMillis()

        val channel =
            mainGuild.createTextChannel("${config.channelPrefix}${config.channelIdentifierType.parse(interaction.user)}") {
                    parentId = config.category.snowflake
                    permissionOverwrites += Overwrite(
                        mainGuild.everyoneRole.id,
                        OverwriteType.Role,
                        allow = Permissions(),
                        deny = Permissions(Permission.ViewChannel)
                    )
                    permissionOverwrites += Overwrite(
                        interaction.user.id,
                        OverwriteType.Member,
                        deny = Permissions(),
                        allow = Permissions(Permission.ViewChannel)
                    )
                    permissionOverwrites += Overwrite(
                        config.supportRole.snowflake,
                        OverwriteType.Role,
                        deny = Permissions(),
                        allow = Permissions(Permission.ViewChannel)
                    )
                }.also {
                    ticket.channelId = it.id.value.toLong()
                    tickets.add(ticket)

                    it.createMessage {
                        config.ticketWelcomeMessage.build(interaction.user)
                    }

                    interaction.respondEphemeral {
                        embed {
                            title = "Ticket | DustreanNET"
                            description =
                                "Your ticket has been created successfully and can be found here: ${it.asChannelOf<TextChannel>().mention}"
                            useDefaultFooter(interaction.user)
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
                                        useDefaultFooter(interaction.user)
                                    }
                                    actionRow {
                                        button(ButtonStyle.Success, InteractionCommandID.RULE_TRIGGER) {
                                            emoji = DiscordPartialEmoji(
                                                config.openEmoji.id?.snowflake,
                                                config.openEmoji.name,
                                                OptionalBoolean.Value(config.openEmoji.animated)
                                            )
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
                                            useDefaultFooter(interaction.user)
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