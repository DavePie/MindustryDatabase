package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configServerIP
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

class RegisterServer(handler: CommandHandler) : BaseServerCommand(handler) {
    companion object {
        init {
            description = "Registers the server into the database."
            parameters = "<server-name>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val name = arguments[0]
        val ip = configServerIP.string()
        val port = Administration.Config.port.num()

        database!!.server().add(ip, port, name)
        restartConfigDependentFeatures()
        Log.info("Server registered to database.")
    }
}