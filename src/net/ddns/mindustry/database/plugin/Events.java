package net.ddns.mindustry.database.plugin;

import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.net.Administration;
import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Server;

import java.util.Optional;

import static net.ddns.mindustry.database.plugin.Configs.configServerIP;
import static net.ddns.mindustry.database.plugin.Main.database;

public class Events {
    protected static void load() {
        arc.Events.on(EventType.PlayerJoin.class, Events::playerJoin);
    }

    private static void playerJoin(EventType.PlayerJoin event) {
        int port = Administration.Config.port.num();
        Optional<Server> server = database.server().find(configServerIP.string(), port);

        Log.debug(String.format("Attempting to add %s to database.", event.player.uuid()));

        if (server.isEmpty()) {
            Log.err("Server is not in database.");
            event.player.kick("Invalid database configuration. Please contact a staff member.");
        }

        AccountQueries.JoinStatus status = database.auth().joinsServer(server.get(), event.player.ip(), event.player.uuid());

        Log.debug(status.getClass());

        if (status instanceof AccountQueries.JoinStatus.NotAuthenticated) {
            Call.infoMessage(event.player.con(), "You are not logged in. Please log in using the " +
                    "[gold]/login[] command.");
        }
    }
}