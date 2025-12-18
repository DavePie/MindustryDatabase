package net.ddns.mindustry.database.plugin

import arc.util.Log
import mindustry.Vars.*
import mindustry.core.GameState
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configServerIP
import net.ddns.mindustry.database.schema.tables.pojos.Server
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private var server: Server? = null
private var scheduler: ScheduledExecutorService? = null

fun startHeartbeatScheduler() {
    if (database == null) {
        Log.warn("No connection to database. Aborting.")
        net.closeServer()
        state.set(GameState.State.menu)
        Log.warn("Aborted. Server is offline now.")
        return
    }

    val possibleServer = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

    if (possibleServer.isEmpty) {
        Log.err(SERVER_IP_PORT_ERROR)
        Log.warn("Due to the above error, the scheduler will not be able to run.")
        return
    }

    server = possibleServer.get()
    scheduler = Executors.newSingleThreadScheduledExecutor()

    scheduler!!.scheduleAtFixedRate(serverHeartbeat(), 1000, server!!.heartbeatPeriod().toLong(), TimeUnit.MILLISECONDS)
}

fun stopHeartbeatScheduler() {
    if ((server == null) || (scheduler == null)) return

    scheduler!!.shutdown()
    scheduler = null
    server = null
}

fun restartHeartbeatScheduler() {
    if ((server == null) || (scheduler == null)) return

    stopHeartbeatScheduler()
    startHeartbeatScheduler()
}

private fun serverHeartbeat(): () -> Unit {
    return {
        Log.debug("Net active: " + net.active())

        if (!net.active()) {
            // pls kys, heartbeat scheduler
            Log.debug("Stopping scheduler.")
            Log.warn("Heartbeat scheduler is still beating, but net is inactive. Assuming improper exit, the heartbeat" +
                    " scheduler will be shut down.")
            stopHeartbeatScheduler()

            exitProcess(0)
        }

        val server = database!!.server().find(configServerIP.string(), Administration.Config.port.num())

        if (server.isEmpty) {
            Log.err(SERVER_IP_PORT_ERROR)
        } else {
            database!!.server().heartbeat(server.get())
        }
    }
}
