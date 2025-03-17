package net.ddns.mindustry.database.plugin

import arc.util.Log
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.schema.tables.pojos.Server
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private var server: Server? = null
private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

fun startHeartbeatScheduler() {
    val possibleServer = database!!.server().find(configServerIP.string(), Administration.Config.port.num());

    if (possibleServer.isEmpty) {
        Log.err(SERVER_IP_PORT_ERROR)
        Log.warn("Due to the above error, the scheduler will not be able to run.")
        return;
    }

    server = possibleServer.get()

    scheduler.scheduleAtFixedRate(serverHeartbeat(), 0, server!!.heartbeatPeriod().toLong(), TimeUnit.MILLISECONDS)
}

fun stopHeartbeatScheduler() {
    scheduler.shutdown()
    server = null
}

fun restartHeartbeatScheduler() {
    if (server != null) stopHeartbeatScheduler()
    startHeartbeatScheduler()
}

private fun serverHeartbeat(): () -> Unit {
    return {
        val server = database!!.server().find(configServerIP.string(), Administration.Config.port.num());

        if (server.isEmpty) {
            Log.err(SERVER_IP_PORT_ERROR)
        } else {
            database!!.server().heartbeat(server.get())
        }
    }
}
