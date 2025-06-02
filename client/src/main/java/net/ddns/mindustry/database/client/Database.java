package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.client.impl.DatabaseImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Database extends AutoCloseable {

    static Database newConnection(String url, String user, String password, SecurityConfig config) {
        return new DatabaseImpl(url, user, password, config);
    }

    SecurityConfig securityConfig();

    AccountQueries account();

    ServerQueries server();

    ServerAccountQueries serverAccount();

    PunishmentQueries punishment();

    RoleQueries role();

    DatabaseEvents events();

    @Override
    void close();
}
