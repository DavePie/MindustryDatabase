package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class ReloadConfigurations(handler: CommandHandler) : ServerCommand(handler) {
    companion object {
        init {
            description = "Reloads any configuration dependent features."
            parameters = ""
        }
    }

    override fun runner(arguments: Array<String>) {
        restartConfigDependentFeatures()
        Log.info("Reloaded configurations.")
    }
}