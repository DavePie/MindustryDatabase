package net.ddns.mindustry.database.plugin.commands

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.SERVER_IP_PORT_ERROR
import net.ddns.mindustry.database.plugin.configPassword
import net.ddns.mindustry.database.plugin.configServerIP
import net.ddns.mindustry.database.plugin.restartConfigDependentFeatures

fun loadServerCommands(handler: CommandHandler) {
    handler.register("register-server", "<server-name>", "Registers the server into the " +
            "database.", ::registerServer)
    handler.register("deregister-server", "[id]", "Deregisters the server from the database.",
        ::deregisterServer)
    handler.register("list-servers", "Lists all servers in the database.", ::listServers)


    handler.register("update-ip", "<new-ip>", "Updates the server's IP in the database.",
        ::updateIP)
    handler.register("update-port", "<new-port>", "Updates the port of the server. You may need" +
            " to restart the server for the changes to take effect.", ::updatePort)
    handler.register("update-name", "<new-name>", "Updates the name of the server. This does " +
            "not affect the name configuration.", ::updateName)


    handler.register("remove-password-config", "Removes the entry for the password configuration.",
        ::removePasswordConfig)
    handler.register("reload-configurations", "Reloads any configuration dependent features.", ::reloadConfigurations)
}

private fun registerServer(args: Array<String>) {
    val name = args[0]
    val ip = configServerIP.string()
    val port = Administration.Config.port.num()

    database!!.server().add(ip, port, name)
    restartConfigDependentFeatures()
    Log.info("Server registered to database.")
}

private fun deregisterServer(args: Array<String>) {
    val currentServer = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

    // I would have a ` && args.length == 0`, but that'll possibly cause issues.
    if (currentServer.isEmpty) {
        Log.err(SERVER_IP_PORT_ERROR)
        return
    }

    var targetID = currentServer.get().id

    if (args.size == 1) {
        targetID = args[0].toInt()
    }

    val possibleTargetServer = database!!.server().find(targetID)

    if (possibleTargetServer.isEmpty && args.isNotEmpty()) {
        Log.err("The server ID provided is invalid.")
        return
    }

    database!!.server().remove(if (args.isNotEmpty()) possibleTargetServer.get() else currentServer.get())
    restartConfigDependentFeatures()
    Log.info("Server deregistered.")
}

private fun listServers(args: Array<String>) { // args is required
    val results = database!!.server().listAll()
    val output = arrayOfNulls<String>(results.size)

    for (i in results.indices) {
        val server = results[i]

        output[i] = String.format(
            """
            %d - %s
            ${'\t'}- IP ${'\t'}${'\t'}%s
            ${'\t'}- PORT ${'\t'}${'\t'}%d
            ${'\t'}- HEARTBEAT ${'\t'}%s
            """.trimIndent(),
            server.id, server.name,
            server.ipAddress.toString(),
            server.port,
            server.heartbeat
        )
    }

    for (toOutput in output) {
        if (toOutput!!.isBlank()) continue
        println(toOutput)
    }
}

private fun updateIP(args: Array<String>) {
    val newIP = args[0]
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

private fun updatePort(args: Array<String>) {
    val newPort = args[0].toInt()
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

private fun updateName(args: Array<String>) {
    val newName = args[0]
    val server = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

    if (server.isEmpty) {
        Log.err(SERVER_IP_PORT_ERROR)
        return
    }

    database!!.server().update(server.get(), null, null, newName)
    Log.info("The name of the server was updated successfully.")
}

private fun removePasswordConfig(args: Array<String>) {
    configPassword.set("")
    restartConfigDependentFeatures()
    Log.info("Removed password configuration.")
}

private fun reloadConfigurations(args: Array<String>) {
    restartConfigDependentFeatures()
    Log.info("Reloaded configurations.")
}
