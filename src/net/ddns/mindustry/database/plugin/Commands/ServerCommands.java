package net.ddns.mindustry.database.plugin.Commands;

import arc.util.CommandHandler;
import arc.util.Log;
import net.ddns.mindustry.database.plugin.Main;
import net.ddns.mindustry.database.plugin.Utilities;
import static net.ddns.mindustry.database.plugin.Main.database;

public class ServerCommands {
    public static void load(CommandHandler handler) {
        handler.register("reconnect", "Reconnects to the database.", ServerCommands::reconnectDatabase);
        handler.register("register-server", "<name> [ip] [port]", "Registers the server to the database.",
                ServerCommands::registerServer);
    }

    public static void reconnectDatabase(String[] args) {
        Main.database = Utilities.newDatabase();

        Log.info("Reconnection successful.");
    }

    private static void registerServer(String[] args) {
        final int port_max = (Short.MAX_VALUE * 2) + 1;

        String name = args[0];
        String ip = "127.0.0.1";
        int port = 6567;

        if (args.length >= 2) {
            ip = args[1];
        }
        if (args.length >= 3) {
            try {
                port = Integer.parseInt(args[2]);
                if (port > port_max || port < 1) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                Log.debug(e);
                Log.err("Could not parse port number. Ensure that the provided port is a valid non-float integer" +
                        " between 1-" + port_max);
            }
        }

        database.server().add(ip, port, name);
        Log.info("Server registered to database.");
    }
}
