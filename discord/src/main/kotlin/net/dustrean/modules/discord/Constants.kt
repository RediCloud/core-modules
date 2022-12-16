package net.dustrean.modules.discord

import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dustrean.api.ICoreAPI
import net.dustrean.modules.discord.data.DiscordConfig

lateinit var coreAPI: ICoreAPI

val configBucket = coreAPI.getRedisConnection().getRedissonClient().getBucket<DiscordConfig>("config:discord-bot")
var config = configBucket.get()

val kordScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

lateinit var kord: Kord

lateinit var mainGuild: Guild
lateinit var teamGuild: Guild