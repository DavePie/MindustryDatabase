package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.*;
import org.jooq.CloseableDSLContext;
import org.jooq.impl.DSL;
import org.postgresql.Driver;
import java.sql.SQLException;
import java.util.Objects;

public final class DatabaseImpl implements Database {

    private final CloseableDSLContext dsl;
    private final SecurityConfig config;
    private final AccountQueriesImpl auth;
    private final ServerQueriesImpl server;
    private final PunishmentQueriesImpl punishment;
    private final RoleQueriesImpl role;
    private final PunishmentListenersImpl punishmentListeners;

    public DatabaseImpl(String url, String username, String password, SecurityConfig config) {

        try { Class.forName(Driver.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The database driver could not be loaded.", e);
        }

        this.dsl = DSL.using(Objects.requireNonNull(url),
                Objects.requireNonNull(username),
                password);

        this.config = Objects.requireNonNull(config);
        this.auth = new AccountQueriesImpl(dsl, config);
        this.server = new ServerQueriesImpl(dsl);
        this.punishment = new PunishmentQueriesImpl(dsl, auth);
        this.role = new RoleQueriesImpl(dsl);
        try { this.punishmentListeners = new PunishmentListenersImpl(dsl);
        } catch (SQLException e) {
            throw new RuntimeException("Could not start the punishment lister task.", e);
        }
    }

    @Override
    public SecurityConfig securityConfig() {
        return config;
    }

    @Override
    public AccountQueries auth() {
        return auth;
    }

    @Override
    public ServerQueries server() {
        return server;
    }

    @Override
    public PunishmentQueries punishment() {
        return punishment;
    }

    @Override
    public PunishmentListeners punishmentListeners() {
        return punishmentListeners;
    }

    @Override
    public RoleQueries role() {
        return role;
    }

    @Override
    public void close() {
        punishmentListeners.close();
        dsl.close();
    }
}
