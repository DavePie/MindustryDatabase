package net.ddns.mindustry.database.plugin.configs

import mindustry.net.Administration.Config

class PluginConfigs : ReloadableConfig() {
    companion object Configs {
        val configURL = Config("url", "The URL for the database.", "")
        val configUser = Config("user", "The user for the database.", "")
        val configPassword = Config("password", "The password for the database user.", "")
        val configAccountLimit = Config(
            "account-limit", "The maximum number of accounts that a player can" +
                    " have.", 5
        )

        val configServerIP = Config(
            "server-ip", "The IP of the server.", "127.0.0.1"
        )
        val configSessionDuration = Config("session-duration", "The maximum duration of a session.", 12)

        val configCommandRateLimit = Config("command-rate-limit", "The amount of time it takes" +
                " between successful commands to kick a player. Value is in seconds. Number of attempts can" +
                " be configured.", 2)
        val configCommandAttempts = Config("command-attempts", "The number of attempts that are" +
                " recorded for the rate limit.", 5)

//        val configRoleConfigPath = Config(
//            "roles-config-path", "The path to the configuration file for roles.",
//            "config/mindustry_database/roles_config.yaml"
//        )
    }

    // These shouldn't really be reloaded. Reloading them will cause duplicate configurations.
    override fun reload() {}
}
