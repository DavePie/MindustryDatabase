package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.configPassword
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class RemovePassword(handler: CommandHandler) : BaseServerCommand(handler) {
    companion object {
        init {
            description = "Removes the entry for the password configuration."
        }
    }

    override fun runner(args: Array<String>) {
        configPassword.set("")
        restartConfigDependentFeatures()
        Log.info("Removed password configuration.")
    }
}