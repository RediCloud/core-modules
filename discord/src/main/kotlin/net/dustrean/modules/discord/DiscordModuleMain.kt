package net.dustrean.modules.discord

import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.builder.kord.KordBuilder
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.launch
import net.dustrean.api.ICoreAPI
import net.dustrean.api.module.Module
import net.dustrean.modules.discord.data.DiscordConfig

@Suppress("unused")
class DiscordModuleMain : Module() {
    override fun onLoad(api: ICoreAPI) {
        if (!configBucket.isExists) {
            val discordConfig = DiscordConfig()
            configBucket.set(discordConfig)
            config = discordConfig
        }
    }

    @OptIn(PrivilegedIntent::class)
    override fun onEnable(api: ICoreAPI) {
        kordScope.launch {
            kord = Kord(config.token) {
                stackTraceRecovery = true
                setupCache()
            }

            kord.on<ReadyEvent> {
                kord.editPresence {
                    status = PresenceStatus.Online
                    playing("on dustrean.net")
                }
            }

            kord.login {
                intents += Intents.all
            }
        }
    }

    override fun onDisable(api: ICoreAPI) {}

    private fun KordBuilder.setupCache() = cache {
        users { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        messages { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 10000))
        }

        members { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.none())
        }

        guilds { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        roles { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        channels { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        voiceState { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }

        emojis { cache, description ->
            MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
        }
    }
}