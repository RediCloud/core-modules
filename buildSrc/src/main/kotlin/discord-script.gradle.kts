plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.kord:kord-core:${BuildConstants.kordVersion}")
    implementation("com.aallam.openai:openai-client:2.1.2")
}