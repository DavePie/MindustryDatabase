package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jspecify.annotations.Nullable;

public interface ConfigQueries {

    void put(String key, String value);

    void put(String key, Server server, String value);

    @Nullable String get(String key);

    @Nullable String get(String key, Server server);
}
