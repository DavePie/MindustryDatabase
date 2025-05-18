package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.SERVER_IP_PORT_ERROR
import net.ddns.mindustry.database.plugin.configServerIP
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class DeregisterServer(handler: CommandHandler) : BaseServerCommand(handler) {
    companion object {
        init {
            description = "Deregisters the server from the database."
            parameters = "[id]"
        }
    }

    override fun runner(arguments: Array<String>) {
        val currentServer = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

        // I would have a ` && args.length == 0`, but that'll possibly cause issues.
        if (currentServer.isEmpty) {
            Log.err(SERVER_IP_PORT_ERROR)
            return
        }

        var targetID = currentServer.get().id

        if (arguments.size == 1) {
            targetID = arguments[0].toInt()
        }

        val possibleTargetServer = database!!.server().find(targetID)

        if (possibleTargetServer.isEmpty && arguments.isNotEmpty()) {
            Log.err("The server ID provided is invalid.")
            return
        }

        database!!.server().remove(if (arguments.isNotEmpty()) possibleTargetServer.get() else currentServer.get())
        restartConfigDependentFeatures()
        Log.info("Server deregistered.")
    }
}