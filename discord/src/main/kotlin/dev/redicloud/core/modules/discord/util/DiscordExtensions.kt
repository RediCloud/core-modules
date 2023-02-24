package dev.redicloud.core.modules.discord.util

import dev.kord.common.entity.Snowflake

val String.snowflake
    get() = Snowflake(this)

val Long.snowflake
    get() = Snowflake(this)