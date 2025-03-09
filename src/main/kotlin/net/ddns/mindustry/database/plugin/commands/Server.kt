package net.ddns.mindustry.database.plugin.commands

import arc.util.CommandHandler
import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.SERVER_IP_PORT_ERROR
import net.ddns.mindustry.database.plugin.configServerIP

fun loadServerCommands(handler: CommandHandler) {
    handler.register("register-server", "<server-name>", "Registers the server into the " +
            "database.", ::registerServer)
    handler.register("deregister-server", "[id]", "Deregisters the server from the database.",
        ::deregisterServer)
    handler.register("list-servers", "Lists all servers in the database.", ::listServers)
}

private fun registerServer(args: Array<String>) {
    val name = args[0]
    val ip = configServerIP.string()
    val port = Administration.Config.port.num()

    database!!.server().add(ip, port, name)
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

    val possibleTargetServer = database!!.server()[targetID]

    if (possibleTargetServer.isEmpty && args.isNotEmpty()) {
        Log.err("The server ID provided is invalid.")
        return
    }

    database!!.server().remove(if (args.isNotEmpty()) possibleTargetServer.get() else currentServer.get())
    Log.info("Server deregistered.")
}

private fun listServers(args: Array<String>) { // args is required
    val results = database!!.server().all
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
