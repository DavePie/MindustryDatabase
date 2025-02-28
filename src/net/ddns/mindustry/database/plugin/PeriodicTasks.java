package net.ddns.mindustry.database.plugin;

import arc.util.Log;
import mindustry.net.Administration;
import net.ddns.mindustry.database.schema.tables.pojos.Server;

import java.util.Optional;
import java.util.concurrent.*;

import static net.ddns.mindustry.database.plugin.Configs.configServerIP;
import static net.ddns.mindustry.database.plugin.Constants.SERVER_IP_PORT_ERROR;
import static net.ddns.mindustry.database.plugin.Main.database;

public class PeriodicTasks {
    private static Server server;
    private static ScheduledExecutorService scheduler;

    public static void load() {
        Optional<Server> possibleServer = database.server().find(configServerIP.string(), Administration.Config.port.num());

        if (possibleServer.isEmpty()) {
            Log.err(SERVER_IP_PORT_ERROR);
            Log.warn("Due to the above error, the scheduler will not be able to run.");
            return;
        }

        server = possibleServer.get();
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(serverHeartbeat(), 0, server.heartbeatPeriod(), TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        scheduler.close();
        server = null;
    }

    public static void reload() {
        stop();
        load();
    }

    // ---------- actual tasks ----------

    private static Runnable serverHeartbeat() {
        return () -> {
            Optional<Server> server = database.server().find(configServerIP.string(), Administration.Config.port.num());

            if (server.isEmpty()) {
                Log.err(SERVER_IP_PORT_ERROR);
                return;
            }

            database.server().heartbeat(server.get());
        };
    }
}
