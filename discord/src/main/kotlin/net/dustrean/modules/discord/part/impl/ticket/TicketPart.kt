package net.dustrean.modules.discord.part.impl.ticket

import net.dustrean.api.ICoreAPI
import net.dustrean.modules.discord.part.DiscordModulePart
import net.dustrean.modules.discord.util.commands.CommandBuilder

object TicketPart : DiscordModulePart() {

    override val name = "Ticket"
    override val commands: List<CommandBuilder> = mutableListOf()
    lateinit var config: TicketConfig

    override suspend fun init() {
        if(ICoreAPI.INSTANCE.getConfigManager().exists("discord:module:ticket")){
            config = ICoreAPI.INSTANCE.getConfigManager().getConfig("discord:module:ticket", TicketConfig::class.java)
        }else{
            config = TicketConfig()
            ICoreAPI.INSTANCE.getConfigManager().createConfig(config)
        }
    }

}