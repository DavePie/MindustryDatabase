package net.ddns.mindustry.database.plugin

import arc.util.CommandHandler
import arc.util.Log
import mindustry.mod.Plugin
import net.ddns.mindustry.database.client.Database
import net.ddns.mindustry.database.plugin.commands.loadServerCommands
import net.ddns.mindustry.database.plugin.commands.client.loadClientCommands
import java.util.logging.LogManager

@Suppress("unused")
class Main : Plugin() {
    companion object {
        var database: Database? = null
    }

    override fun init() {
        // I'm going to kill myself if jOOQ sends another self-ad
        LogManager.getLogManager().reset()
        // https://stackoverflow.com/a/5762502
        Log.info("\u001B[34mPowered by jOOQ.\u001B[0m")

        loadEvents()
        loadChatFilters()
        restartConfigDependentFeatures()
        Log.info("Database plugin loaded.")
        Log.warn("Do NOT run `exit` when hosting. Instead, stop the server with `stop` and then `exit`. This ensures " +
                "that the scheduler is closed gracefully.")
    }

    override fun registerClientCommands(handler: CommandHandler?) {
        loadClientCommands(handler!!)
    }

    override fun registerServerCommands(handler: CommandHandler?) {
        loadServerCommands(handler!!)
    }
}