plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
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
    fun pluginDep(id: String, version: String) = "${id}:${id}.gradle.plugin:${version}"
    val kotlinVersion = "1.8.0"

    implementation(pluginDep("net.dustrean.libloader", "1.6.2"))

    compileOnly(kotlin("gradle-plugin", embeddedKotlinVersion))
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))

    implementation(pluginDep("io.papermc.paperweight.userdev", "1.4.0"))
}