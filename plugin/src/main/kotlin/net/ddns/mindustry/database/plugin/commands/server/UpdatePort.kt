package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.SERVER_IP_PORT_ERROR
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configServerIP
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class UpdatePort(handler: CommandHandler) : BaseServerCommand(handler) {
    companion object {
        init {
            description = "Updates the port of the server. You may need to restart the server for the changes to " +
                    "take effect."
            parameters = "<new-port>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val newPort = arguments[0].toInt()
        val server = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

        if (server.isEmpty) {
            Log.err(SERVER_IP_PORT_ERROR)
            return
        }

        database!!.server().update(server.get(), null, newPort, null)
        Administration.Config.port.set(newPort)
        restartConfigDependentFeatures()

        Log.info("Port updated. Keep in mind that you may need to restart the server for these changes to take" +
                " effect.")
    }
}