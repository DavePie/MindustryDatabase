package net.ddns.mindustry.database.javaPlugin;

import arc.util.Log;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Administration;
import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Server;

import java.util.Optional;

import static net.ddns.mindustry.database.javaPlugin.Configs.configServerIP;
import static net.ddns.mindustry.database.javaPlugin.Main.database;

public class Events {
    protected static void load() {
        arc.Events.on(EventType.PlayerConnect.class, Events::playerConnect);
        arc.Events.on(EventType.PlayerLeave.class, Events::playerLeave);
    }

    private static void playerConnect(EventType.PlayerConnect event) {
        int port = Administration.Config.port.num();
        Optional<Server> server = database.server().find(configServerIP.string(), port);

        if (server.isEmpty()) {
            Log.err("Server is not in the database.");
            event.player.kick("Invalid database configuration. Please contact a staff member.");
            return;
        }

        AccountQueries.JoinStatus status = database.auth().joinsServer(server.get(), event.player.ip(), event.player.uuid());

        if (status instanceof AccountQueries.JoinStatus.NotAuthenticated) {
            Call.infoMessage(event.player.con(), "You are not logged in. Please log in using the " +
                    "[gold]/login[] command or signup with the [gold]/signup[] command.");
            event.player.team(Team.derelict);
            return;
        } else if (status instanceof AccountQueries.JoinStatus.AlreadyInServer) {
            event.player.kick("You're already in one of the servers!", 0);
            return;
        } else if (status instanceof AccountQueries.JoinStatus.NotAuthorized) {
            event.player.kick("You're not authorized to join the server.");
            return;
        } else if (status instanceof AccountQueries.JoinStatus.Joined) {
            String displayName = ((AccountQueries.JoinStatus.Joined) status).account().displayName();
            event.player.name(displayName);
            event.player.sendMessage("[gold]Welcome back to the server!");
            return;
        }

        event.player.kick("An unknown issue has occurred and you're unable to join the server. Please contact an" +
                " admin or a staff member of this server.");
        Log.warn("JoinStatus returned a status that is not accounted for.");
        Log.warn(String.format("Returned status: %s", status.getClass()));
    }

    private static void playerLeave(EventType.PlayerLeave event) {
        Optional<Account> account = database.auth().find(event.player.ip(), event.player.uuid());

        if (account.isEmpty()) {
            Log.warn("A player left but they could not be found in the database. They may not have a session.");
            return;
        }

        database.auth().leavesServer(account.get());
    }
}