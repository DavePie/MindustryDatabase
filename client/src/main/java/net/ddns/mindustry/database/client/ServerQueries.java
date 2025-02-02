package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Server;
import java.util.Optional;

public interface ServerQueries {

    Optional<Server> find(String ip, int port);

    /**
     * Adds a server to the database.
     * @param ip The IP of the server.
     * @param port The port of the server.
     * @param name The name of the server.
     */
    void add(String ip, int port, String name);
}
