plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.redicloud.dev/snapshots")
    maven("https://repo.redicloud.dev/releases")
}

dependencies {
    fun pluginDep(id: String, version: String) = "${id}:${id}.gradle.plugin:${version}"
    val kotlinVersion = "1.8.0"

    implementation(pluginDep("dev.redicloud.libloader", "1.6.3"))

    compileOnly(kotlin("gradle-plugin", embeddedKotlinVersion))
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))

    implementation(pluginDep("io.papermc.paperweight.userdev", "1.4.0"))
}