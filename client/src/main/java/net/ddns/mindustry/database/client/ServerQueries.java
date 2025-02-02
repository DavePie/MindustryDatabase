package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Server;
import java.util.Optional;

public interface ServerQueries {

    Optional<Server> find(String ip, int port);
}
