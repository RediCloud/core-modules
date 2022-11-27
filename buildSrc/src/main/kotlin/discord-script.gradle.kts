plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.kord:kord-core:${BuildConstants.kordVersion}")
}