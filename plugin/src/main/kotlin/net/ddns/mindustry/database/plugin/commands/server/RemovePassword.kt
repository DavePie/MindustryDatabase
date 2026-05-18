package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class RemovePassword(handler: CommandHandler) : ServerCommand(handler) {
    companion object {
        init {
            description = "Removes the entry for the password configuration."
        }
    }

    override fun runner(args: Array<String>) {
        Log.warn("This command is deprecated. It will NOT do anything. Please edit your config.toml to change the" +
                " password.")
//        configPassword.set("")
//        restartConfigDependentFeatures()
//        Log.info("Removed password configuration.")
    }
}