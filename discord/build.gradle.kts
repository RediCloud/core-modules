import net.dustrean.libloader.plugin.LibraryLoader

plugins {
    `kotlin-script`
    `discord-script`
}

the(LibraryLoader.LibraryLoaderConfig::class).apply {
    mainClass.set("net.dustrean.modules.discord.DiscordModuleMainKt")
}