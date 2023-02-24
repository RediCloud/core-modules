import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("dev.redicloud.libloader")
}

repositories {
    mavenCentral()
    maven("https://repo.redicloud.dev/snapshots")
    maven("https://repo.redicloud.dev/releases")
}

dependencies {
    implementation("dev.redicloud.api:api:${BuildConstants.coreVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${BuildConstants.coroutinesVersion}")
    implementation("org.redisson:redisson:${BuildConstants.redissonVersion}")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<JavaCompile> {
        options.release.set(17)
        options.encoding = "UTF-8"
    }
}