package net.ddns.mindustry.database.plugin

import arc.Events
import arc.util.Log
import mindustry.core.GameState
import mindustry.game.EventType.PlayerConnect
import mindustry.game.EventType.PlayerLeave
import mindustry.game.EventType.PlayEvent
import mindustry.game.EventType.StateChangeEvent
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.net.Administration
import net.ddns.mindustry.database.client.ServerAccountQueries
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configServerIP

fun loadEvents() {
    Events.on(PlayerConnect::class.java) {e -> playerConnect(e)}
    Events.on(PlayerLeave::class.java) {e -> playerLeave(e)}
    Events.on(PlayEvent::class.java) {e -> startHeartbeatScheduler()}
    Events.on(StateChangeEvent::class.java) { e -> gameOver(e)}
}

private fun playerConnect(event: PlayerConnect) {
    val port = Administration.Config.port.num()
    val server = database!!.server().find(configServerIP.string(), port)

    if (server.isEmpty) {
        Log.err("Server is not in the database.")
        event.player.kick("Invalid database configuration. Please contact a staff member.")
        return
    }

    val status = database!!.serverAccount().joinsServer(server.get(), event.player.name(), event.player.ip(), event.player.uuid())

    when (status) {
        is ServerAccountQueries.JoinStatus.NotAuthenticated -> {
            Call.infoMessage(event.player.con(), "You are not logged in. Please log in using the [gold]/login[]" +
                    " command or signup with the [gold]/signup[] command.")
            event.player.team(Team.derelict)
        }

        is ServerAccountQueries.JoinStatus.AlreadyInServer -> event.player.kick("You're already in one of the servers!", 0)

        is ServerAccountQueries.JoinStatus.NotWhitelisted -> event.player.kick("You're not whitelisted in this server.", 0)

        is ServerAccountQueries.JoinStatus.Joined -> {
            event.player.sendMessage("[gold]Welcome back to the server!")
        }
    }
}

private fun playerLeave(event: PlayerLeave) {
    val account = database!!.account().find(event.player.ip(), event.player.uuid())

    if (account.isEmpty) {
        Log.warn("A player left but they could not be found in the database. They may not have a session.")
        return
    }
    val port = Administration.Config.port.num()
    val server = database!!.server().find(configServerIP.string(), port)
    database!!.serverAccount().leavesServer(account.get(), server.get())
}

private fun gameOver(event: StateChangeEvent) {
    Log.debug("State change detected")
    Log.debug("New state: " + event.to.name)
    Log.debug("Old state: " + event.from.name)

    if (event.to == GameState.State.playing) {
        return
    }

    Log.debug("Killing heartbeat scheduler due to non-playing state.")
    stopHeartbeatScheduler()
}
