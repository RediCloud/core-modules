import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://repo.dustrean.net/snapshots") {
        credentials {
            username = findProperty("dustreanUsername") as String
            password = findProperty("dustreanPassword") as String
        }
    }
    maven("https://repo.dustrean.net/releases") {
        credentials {
            username = findProperty("dustreanUsername") as String
            password = findProperty("dustreanPassword") as String
        }
    }
}

dependencies {
    implementation("net.dustrean:api:${BuildConstants.coreVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${BuildConstants.coroutinesVersion}")
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