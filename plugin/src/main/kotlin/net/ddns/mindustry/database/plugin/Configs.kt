package net.ddns.mindustry.database.plugin

import mindustry.net.Administration.Config

val configURL = Config("url", "The URL for the database.", "")
val configUser = Config("user", "The user for the database.", "")
val configPassword = Config("password", "The password for the database user.", "")
val configAccountLimit = Config("account-limit", "The maximum number of accounts that a player can" +
        " have.", 5)

val configServerIP = Config("server-ip", "The IP of the server.", "127.0.0.1")
val configSessionDuration = Config("session-duration", "The maximum duration of a session.", 12)
