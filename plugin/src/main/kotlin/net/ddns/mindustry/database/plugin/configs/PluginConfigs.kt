package net.ddns.mindustry.database.plugin.configs

import mindustry.net.Administration.Config

class PluginConfigs : ReloadableConfig() {
    companion object Configs {
        lateinit var configURL: Config
        lateinit var configUser: Config
        lateinit var configPassword: Config
        lateinit var configAccountLimit: Config

        lateinit var configServerIP: Config
        lateinit var configSessionDuration: Config

        lateinit var configRoleConfigPath: Config
    }

    override fun reload() {
        configURL = Config("url", "The URL for the database.", "")
        configUser = Config("user", "The user for the database.", "")
        configPassword = Config("password", "The password for the database user.", "")
        configAccountLimit = Config(
            "account-limit", "The maximum number of accounts that a player can" +
                    " have.", 5
        )

        configServerIP = Config(
            "server-ip", "The IP of the server.",
            "127.0.0.1"
        )
        configSessionDuration = Config("session-duration", "The maximum duration of a session.", 12)

        configRoleConfigPath = Config(
            "roles-config-path", "The path to the configuration file for roles.",
            "config/mindustry_database/roles_config.yaml"
        )
    }
}
