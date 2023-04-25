import gradle.kotlin.dsl.accessors._5b841d749b44c4d634f7cfea3ed45134.implementation

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.redicloud.dev/snapshots")
    maven("https://repo.redicloud.dev/releases")
}

dependencies {
    implementation("com.velocitypowered:velocity-api:${BuildConstants.velocityAPIVersion}")

    implementation("dev.redicloud.clients:velocity:1.0.0-SNAPSHOT")
}