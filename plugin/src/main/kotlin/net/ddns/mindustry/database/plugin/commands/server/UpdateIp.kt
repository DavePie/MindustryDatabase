package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.SERVER_IP_PORT_ERROR
import net.ddns.mindustry.database.plugin.configServerIP
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class UpdateIp(handler: CommandHandler) : BaseServerCommand(handler) {
    companion object {
        init {
            description = "Updates the server's IP in the database."
            parameters = "<new-ip>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val newIP = arguments[0]
        val server = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

        if (server.isEmpty) {
            Log.err(SERVER_IP_PORT_ERROR)
            return
        }

        database!!.server().update(server.get(), newIP, null, null)
        configServerIP.set(newIP)
        restartConfigDependentFeatures()

        Log.info("The IP of the server was updated successfully.")
    }
}