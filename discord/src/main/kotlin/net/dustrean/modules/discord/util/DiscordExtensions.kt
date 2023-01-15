package net.dustrean.modules.discord.util

import dev.kord.common.entity.Snowflake

val String.toSnowflake
    get() = Snowflake(this)

val Long.toSnowflake
    get() = Snowflake(this)