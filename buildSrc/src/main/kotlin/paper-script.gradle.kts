plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

repositories {
    mavenCentral()
    maven("https://repo.redicloud.dev/snapshots")
    maven("https://repo.redicloud.dev/releases")
}

dependencies {
    paperDevBundle("${BuildConstants.minecraftVersion}-R0.1-SNAPSHOT")

    implementation("dev.redicloud.clients:paper:1.0.0-SNAPSHOT")

    implementation("net.kyori:adventure-api:${BuildConstants.adventureVersion}")
    implementation("net.kyori:adventure-text-minimessage:${BuildConstants.adventureVersion}")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}