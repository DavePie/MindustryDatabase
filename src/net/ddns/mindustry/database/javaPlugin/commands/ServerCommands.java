package net.ddns.mindustry.database.javaPlugin.commands;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.net.Administration;
import net.ddns.mindustry.database.javaPlugin.Utilities;
import net.ddns.mindustry.database.schema.tables.pojos.Server;

import java.util.List;
import java.util.Optional;

import static net.ddns.mindustry.database.javaPlugin.Configs.configServerIP;
import static net.ddns.mindustry.database.javaPlugin.Constants.SERVER_IP_PORT_ERROR;
import static net.ddns.mindustry.database.javaPlugin.Main.database;

public class ServerCommands {
    public static void load(CommandHandler handler) {
        // ---- Direct database stuff ----

        handler.register("reload-configs", "Reloads anything that is dependent on the configuration of" +
                " the server.", ServerCommands::reloadConfigs);
        handler.register("register-server", "<name>", "Registers the server to the database.",
                ServerCommands::registerServer);
        handler.register("deregister-server", "[id]", "Deregisters the server from the database.",
                ServerCommands::deregisterServer);
        handler.register("update-ip", "<new-ip>", "Updates the IP of the server. This will edit" +
                        " both the configuration and the database entry.", ServerCommands::updateIP);
        handler.register("update-port", "<new-port>", "Updates the port of the server. This will" +
                " edit both the configuration and the database entry.", ServerCommands::updatePort);
        handler.register("update-name", "<new-name>", "Updates the name of the server. Will NOT " +
                "update the configuration.", ServerCommands::updateName);
        handler.register("list-servers", "Lists the servers registered in the database.",
                ServerCommands::fetchAllServers);
        handler.register("heartbeat-debug", "Sends a heartbeat to the database. DEBUG ONLY.",
                ServerCommands::heartbeatDebug);
    }

    private static void reloadConfigs(String[] args) {
        Utilities.restartConfigDependentFeatures();

        Log.info("Finished reloading configurations.");
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

        // I would have a ` && args.length == 0`, but that'll possibly cause issues.
        if (currentServer.isEmpty()) {
            Log.err(SERVER_IP_PORT_ERROR);
            return;
        }

        int targetID = currentServer.get().id();

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
        Utilities.restartConfigDependentFeatures();

        Log.info("The IP of the server was updated successfully.");
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
        Utilities.restartConfigDependentFeatures();

        Log.info("Port updated. Keep in mind that you may need to restart the server for these changes to take" +
                " effect.");
    }

    private static void updateName(String[] args) {
        String newName = args[0];
        Optional<Server> server = database.server().find(configServerIP.string(), Administration.Config.port.num());

        if (server.isEmpty()) {
            Log.err(SERVER_IP_PORT_ERROR);
            return;
        }

        database.server().update(server.get(), null, null, newName);
        Log.info("The name of the server was updated successfully.");
    }

    ///  If it ain't broke, then don't fix it.
    public static void fetchAllServers(String[] args) {
        List<Server> results = database.server().getAll();
        String[] output = new String[results.size()];

        for (int i = 0; i < results.size(); i++) {
            Server server = results.get(i);

            output[i] = String.format(
                    """
                    %d - %s
                    \t- IP \t\t%s
                    \t- PORT \t\t%d
                    \t- HEARTBEAT \t%s
                    """,
                    server.id(), server.name(),
                    server.ipAddress().toString(),
                    server.port(),
                    server.heartbeat()
            );
        }

        for (String toOutput : output) {
            if (toOutput.isBlank()) continue;
            System.out.println(toOutput);
        }
    }

    public static void heartbeatDebug(String[] args) {
        if (!Administration.Config.debug.bool()) {
            Log.warn("This is a command intended for debugging and is not meant to be used in a production" +
                    " environment. If you truly do wish to run this command, then run `config debug true` to enable" +
                    " debugging.");
            return;
        }

        Optional<Server> server = database.server().find(configServerIP.string(), Administration.Config.port.num());

        if (server.isEmpty()) {
            Log.err(SERVER_IP_PORT_ERROR);
            return;
        }

        database.server().heartbeat(server.get());
    }
}
