package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.*;
import org.jooq.CloseableDSLContext;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Inet;
import org.jspecify.annotations.NullMarked;
import org.postgresql.Driver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Objects;

@NullMarked
public final class DatabaseImpl implements Database {

    private final CloseableDSLContext dsl;
    private final SecurityConfig config;
    private final AccountQueriesImpl auth;
    private final ServerQueriesImpl server;
    private final ServerAccountQueriesImpl serverAccount;
    private final PunishmentQueriesImpl punishment;
    private final RoleQueriesImpl role;
    private final DatabaseEventsImpl punishmentListeners;

    public DatabaseImpl(String url, String username, String password, SecurityConfig config) {

        try { Class.forName(Driver.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The database driver could not be loaded.", e);
        }

        this.dsl = DSL.using(Objects.requireNonNull(url),
                Objects.requireNonNull(username),
                password);

        this.config = Objects.requireNonNull(config);
        this.auth = new AccountQueriesImpl(this);
        this.server = new ServerQueriesImpl(this);
        this.serverAccount = new ServerAccountQueriesImpl(this);
        this.punishment = new PunishmentQueriesImpl(this);
        this.role = new RoleQueriesImpl(this);
        // Must be last since it uses the classes above during initialization.
        try { this.punishmentListeners = new DatabaseEventsImpl(this);
        } catch (SQLException e) {
            throw new RuntimeException("Could not start the punishment lister task.", e);
        }
    }

    public static Inet inet(String ip) throws IllegalArgumentException {
        Objects.requireNonNull(ip);
        try { return Inet.inet(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("The provided IP address is invalid.", e);
        }
    }

    public DSLContext dsl() {
        return dsl;
    }

    @Override
    public SecurityConfig securityConfig() {
        return config;
    }

    @Override
    public AccountQueriesImpl account() {
        return auth;
    }

    @Override
    public ServerQueriesImpl server() {
        return server;
    }

    @Override
    public ServerAccountQueriesImpl serverAccount() {
        return serverAccount;
    }

    @Override
    public PunishmentQueriesImpl punishment() {
        return punishment;
    }

    @Override
    public RoleQueriesImpl role() {
        return role;
    }

    @Override
    public DatabaseEventsImpl events() {
        return punishmentListeners;
    }

    @Override
    public void close() {
        punishmentListeners.close();
        dsl.close();
    }
}
