plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

repositories {
    mavenCentral()
}

dependencies {
    paperDevBundle("${BuildConstants.minecraftVersion}-R0.1-SNAPSHOT")

    implementation("net.kyori:adventure-api:${BuildConstants.adventureVersion}")
    implementation("net.kyori:adventure-text-minimessage:${BuildConstants.adventureVersion}")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}