import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("net.dustrean.libloader")
}

repositories {
    mavenCentral()
    maven("https://repo.dustrean.net/snapshots") {
        credentials {
            username = findProperty("DUSTREAN_REPO_USERNAME") as String? ?: System.getenv("DUSTREAN_REPO_USERNAME")
            password = findProperty("DUSTREAN_REPO_PASSWORD") as String? ?: System.getenv("DUSTREAN_REPO_PASSWORD")
        }
    }
    maven("https://repo.dustrean.net/releases") {
        credentials {
            username = findProperty("DUSTREAN_REPO_USERNAME") as String? ?: System.getenv("DUSTREAN_REPO_USERNAME")
            password = findProperty("DUSTREAN_REPO_PASSWORD") as String? ?: System.getenv("DUSTREAN_REPO_PASSWORD")
        }
    }
}

dependencies {
    implementation("net.dustrean.api:api:${BuildConstants.coreVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${BuildConstants.coroutinesVersion}")
    implementation("org.redisson:redisson:${BuildConstants.redissonVersion}")
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