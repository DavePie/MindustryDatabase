package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.client.impl.DatabaseImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Database extends AutoCloseable {

    static Database newConnection(String url, String user, String password, SecurityConfig config) {
        return new DatabaseImpl(url, user, password, config);
    }

    SecurityConfig securityConfig();

    AccountQueries auth();

    ServerQueries server();

    PunishmentQueries punishment();

    PunishmentListeners punishmentListeners();

    RoleQueries role();

    @Override
    void close();
}
