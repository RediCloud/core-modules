plugins {
    `kotlin-script`
    `paper-script`
    `minestom-script`
    `velocity-script`
    kotlin("jvm")
}
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.WARN
    archiveFileName.set("${project.name}.jar")
}