package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.client.impl.DatabaseImpl;

public interface Database extends AutoCloseable {

    static Database newConnection(String url, String user, String password, SecurityConfig config) {
        return new DatabaseImpl(url, user, password, config);
    }

    SecurityConfig securityConfig();

    AccountQueries auth();

    ServerQueries server();

    PunishmentQueries punishment();

    RoleQueries role();

    @Override
    void close();
}
