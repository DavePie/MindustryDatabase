package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import net.ddns.mindustry.database.plugin.Main.Companion.database

class ListServers(handler: CommandHandler) : ServerCommand(handler) {
    companion object {
        init {
            description = "Lists all servers in the database."
            parameters = ""
        }
    }

    override fun runner(arguments: Array<String>) {
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
}