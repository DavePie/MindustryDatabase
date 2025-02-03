package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Server;
import java.util.Optional;

public interface ServerQueries {
    /**
     * Finds a server in the database.
     * @param ip The IP of the server to find.
     * @param port The port of the server to find.
     * @return An optional that may contain the server object.
     */
    Optional<Server> find(String ip, int port);

    /**
     * Adds a server entry to the database.
     * @param ip The IP of the server.
     * @param port The port of the server.
     * @param name The name of the server.
     */
    void add(String ip, int port, String name);

    /**
     * Updates a server's entry.
     * @param server The server entry to update.
     * @param newIP The new IP.
     * @param newPort The new port.
     * @param newName The new name.
     */
    void update(Server server, String newIP, int newPort, String newName);

    /**
     * Runs a heartbeat on the server.
     * @param server The server whose heart will beat.
     */
    void heartbeat(Server server);

    /**
     * Removes a server entry from the database.
     * @param server The server entry to remove from the database.
     */
    void remove(Server server);
}
