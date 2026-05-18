package net.ddns.mindustry.database.plugin.configs.toml

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseInfo(
    val url: String,
    val username: String,
    val password: String
)