package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.SERVER_IP_PORT_ERROR
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configServerIP

class UpdateName(handler: CommandHandler) : ServerCommand(handler) {
    companion object {
        init {
            description = "Updates the name of the server. This does not affect the name configuration."
            parameters = "<new-name>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val newName = arguments[0]
        val server = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

        if (server.isEmpty) {
            Log.err(SERVER_IP_PORT_ERROR)
            return
        }

        database!!.server().update(server.get(), null, null, newName)
        Log.info("The name of the server was updated successfully.")
    }
}
