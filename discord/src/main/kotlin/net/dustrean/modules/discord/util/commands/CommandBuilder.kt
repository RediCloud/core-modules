package net.dustrean.modules.discord.util.commands

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake

@CommandAnnotations.TopLevel.CommandDsl
interface CommandBuilder {
    val name: String
    val guildID: Snowflake?
    var permissions: Permissions

    suspend fun create()
}

class CommandAnnotations {
    class TopLevel {
        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
        @DslMarker
        annotation class CommandDsl
    }

    class BuildLevel {
        @DslMarker
        annotation class RunsDsl

        @DslMarker
        annotation class ConfigDsl
    }
}