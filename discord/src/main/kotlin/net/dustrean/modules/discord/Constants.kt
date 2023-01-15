package net.dustrean.modules.discord

import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dustrean.api.ICoreAPI
import net.dustrean.api.config.IConfigManager
import net.dustrean.modules.discord.data.DiscordConfig

lateinit var coreAPI: ICoreAPI

lateinit var configManager: IConfigManager
lateinit var config: DiscordConfig

val kordScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
val ioScope = CoroutineScope(Dispatchers.IO)

lateinit var kord: Kord

lateinit var mainGuild: Guild
lateinit var teamGuild: Guild
lateinit var logChannel: TextChannel