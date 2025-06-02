package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Optional;

@NullMarked
public interface ServerQueries {

    /**
     * Gets a server via its ID.
     * @param id The ID of the server.
     * @return An optional that may contain the server object.
     */
    Optional<Server> find(int id);

    /**
     * Finds a server in the database.
     * @param ip The IP of the server to find.
     * @param port The port of the server to find.
     * @return An optional that may contain the server object.
     */
    Optional<Server> find(String ip, int port);

    List<Server> listAll();

    /**
     * Adds a server entry to the database.
     * @param ip The IP of the server.
     * @param port The port of the server.
     * @param name The name of the server.
     * @return true if the server has been added, false if the server with these parameters is already present.
     */
    boolean add(String ip, int port, String name);

    /**
     * Updates a server's entry.
     * @param server The server entry to update.
     * @param newIP The new IP.
     * @param newPort The new port.
     * @param newName The new name.
     */
    void update(Server server, @Nullable String newIP, @Nullable Integer newPort, @Nullable String newName);

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

    void modifyWhitelist(Server server, boolean enabled);

    boolean isWhitelistEnabled(Server server);
}
