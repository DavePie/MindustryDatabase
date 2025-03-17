package net.ddns.mindustry.database.plugin

import arc.util.Log
import net.ddns.mindustry.database.client.Database
import net.ddns.mindustry.database.client.SecurityConfig
import net.ddns.mindustry.database.plugin.Main.Companion.database
import java.security.NoSuchAlgorithmException

/**
 * Makes and returns a new `Database` object.
 * @return `Database`
 */
fun newDatabase(): Database? {
    val database: Database
    val securityConfig: SecurityConfig

    try {
        securityConfig = SecurityConfig(
            "SHA-256", 32, 255, 10,
            69000, 8
        )
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
    }

    try {
        database = Database.newConnection(
            "jdbc:postgresql://" + configURL.string() + "/mindustry_database",
            configUser.string(), configPassword.string(), securityConfig
        )
    } catch (e: Exception) {
        Log.debug(e)
        Log.warn("Ensure that the URL, the user, and the user's password is correct.")
        return null
    }

    return database
}

/**
 * Restarts anything that is dependent upon the IP and port configurations of the server. This is always ran at
 * plugin initialization.
 */
fun restartConfigDependentFeatures() {
    database = newDatabase()

    if (database == null) {
        Log.warn("Database connection cannot be established.")
        Log.warn("Skipping configuration dependent features since database is null. If the configurations are" +
                    " correct, then reload the configurations."
        )
        return
    }

    restartHeartbeatScheduler()
}
