plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.theholywaffle:teamspeak3-api:${BuildConstants.teamspeakAPIVersion}")
}