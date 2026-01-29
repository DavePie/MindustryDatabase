package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jspecify.annotations.Nullable;

public interface ConfigQueries {

    /// Creates a new global configuration.\
    /// If a configuration already exists, the previous `value` will be replaced with the new one.
    /// @param key a unique key to identify the configuration.
    /// @param value the JSON value.
    void put(String key, String value);

    /// Creates a new server configuration.\
    /// If a configuration already exists, the previous `value` will be replaced with the new one.
    /// @param key a unique key to identify the configuration.
    /// @param server the server this configuration goes under.
    /// @param value the JSON value.
    /// @apiNote The `key` + `server` is unique, meaning the same key can be used on different servers to save different values.
    void put(String key, Server server, String value);

    /// Retrieves the global configuration `value` from the provided key.
    /// @param key a unique key to identify the configuration.
    /// @return the saved `value` if it exists, otherwise null.
    @Nullable String get(String key);

    /// Retrieves the server configuration `value` from the provided key.
    /// @param key a unique key to identify the configuration.
    /// @param server the server the configuration is stored under.
    /// @return the saved `value` if it exists, otherwise null.
    @Nullable String get(String key, Server server);
}
