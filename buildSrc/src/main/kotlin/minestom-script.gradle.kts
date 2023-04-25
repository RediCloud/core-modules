plugins {
    kotlin("jvm")
}

repositories {
    maven("https://jitpack.io")
    maven("https://repo.redicloud.dev/snapshots")
    maven("https://repo.redicloud.dev/releases")
}

dependencies {
    implementation("com.github.Minestom.Minestom:Minestom:${BuildConstants.minestomVersion}")

    implementation("dev.redicloud.clients:minestom:1.0.0-SNAPSHOT")
}