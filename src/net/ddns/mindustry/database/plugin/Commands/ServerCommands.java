package net.ddns.mindustry.database.plugin.Commands;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.net.Administration;
import net.ddns.mindustry.database.plugin.Main;
import net.ddns.mindustry.database.plugin.Utilities;
import net.ddns.mindustry.database.schema.tables.pojos.Server;

import java.util.Optional;

import static net.ddns.mindustry.database.plugin.Configs.configServerIP;
import static net.ddns.mindustry.database.plugin.Main.database;

public class ServerCommands {
    protected static final String SERVER_IP_PORT_ERROR = "The server configuration is invalid. Ensure that the server" +
            " configuration in both the database and the config are valid.";

    public static void load(CommandHandler handler) {
        handler.register("reconnect", "Reconnects to the database.", ServerCommands::reconnectDatabase);
        handler.register("register-server", "<name>", "Registers the server to the database.",
                ServerCommands::registerServer);
        handler.register("deregister-server", "[id]", "Deregisters the server from the database.",
                ServerCommands::deregisterServer);
        handler.register("update-ip", "<new-ip>", "Updates the IP of the server. This will edit" +
                        " both the configuration and the database entry.", ServerCommands::updateIP);
        handler.register("update-port", "<new-port>", "Updates the port of the server. This will " +
                "edit both the configuration and the database entry.", ServerCommands::updatePort);
    }

    public static void reconnectDatabase(String[] args) {
        Main.database = Utilities.newDatabase();

        Log.info("Reconnection successful.");
    }

    private static void registerServer(String[] args) {
        String name = args[0];
        String ip = configServerIP.string();
        int port = Administration.Config.port.num();

        database.server().add(ip, port, name);
        Log.info("Server registered to database.");
    }

    private static void deregisterServer(String[] args) {
        Optional<Server> currentServer = database.server().find(configServerIP.string(), Administration.Config.port.num());

        if (currentServer.isEmpty() && args.length == 0) {
            Log.err(SERVER_IP_PORT_ERROR);
            return;
        }

        int targetID = currentServer.get().id().intValue();

        if (args.length == 1) {
            targetID = Integer.parseInt(args[0]);
        }

        Optional<Server> possibleTargetServer = database.server().get(targetID);

        if (possibleTargetServer.isEmpty() && args.length >= 1) {
            Log.err("The server ID provided is invalid.");
            return;
        }

        database.server().remove(args.length >= 1 ? possibleTargetServer.get() : currentServer.get());
    }

    private static void updateIP(String[] args) {
        String newIP = args[0];
        Optional<Server> server = database.server().find(configServerIP.string(), Administration.Config.port.num());

        if (server.isEmpty()) {
            Log.err(SERVER_IP_PORT_ERROR);
            return;
        }

        database.server().update(server.get(), newIP, null, null);
        configServerIP.set(newIP);
        Log.info("The IP of the server is updated.");
    }

    private static void updatePort(String[] args) {
        int newPort = Integer.parseInt(args[0]);
        Optional<Server> server = database.server().find(configServerIP.string(), Administration.Config.port.num());

        if (server.isEmpty()) {
            Log.err(SERVER_IP_PORT_ERROR);
            return;
        }

        database.server().update(server.get(), null, newPort, null);
        Administration.Config.port.set(newPort);
        Log.info("Port updated. Keep in mind that you may need to restart the server for these changes to take" +
                " effect.");
    }
}
