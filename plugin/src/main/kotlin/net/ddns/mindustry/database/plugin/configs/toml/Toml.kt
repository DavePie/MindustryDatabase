package net.ddns.mindustry.database.plugin.configs.toml

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.decodeFromString
import mindustry.Vars.dataDirectory

var databaseInfo: DatabaseInfo? = null

fun loadToml() {
    val configToml = dataDirectory.child("config.toml")
    val config = TomlInputConfig(
        ignoreUnknownNames = false,
        allowEmptyValues = true,
        allowNullValues = false,
        allowEscapedQuotesInLiteralStrings = true,
        allowEmptyToml = false,
        ignoreDefaultValues = true,
    )
    val toml = Toml(config)

    databaseInfo = toml.decodeFromString<DatabaseInfo>(configToml.readString())
}
