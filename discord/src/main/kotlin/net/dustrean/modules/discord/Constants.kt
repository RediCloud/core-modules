package net.dustrean.modules.discord

import dev.kord.core.Kord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dustrean.api.ICoreAPI
import net.dustrean.modules.discord.data.DiscordConfig

val coreAPI: ICoreAPI = ICoreAPI.INSTANCE

val configBucket = coreAPI.getRedisConnection().getRedissonClient().getBucket<DiscordConfig>("config:discord-bot")
var config = configBucket.get()

val kordScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

lateinit var kord: Kord