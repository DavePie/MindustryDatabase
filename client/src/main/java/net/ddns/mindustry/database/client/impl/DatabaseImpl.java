package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.*;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.mariadb.jdbc.Driver;
import java.util.Objects;

public final class DatabaseImpl implements Database {

    private final SecurityConfig config;
    private final AccountQueriesImpl auth;
    private final ServerQueriesImpl server;
    private final PunishmentQueriesImpl punishment;

    public DatabaseImpl(String url, String username, String password, SecurityConfig config) {

        try { Class.forName(Driver.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The mariadb driver could not be loaded.", e);
        }

        final DSLContext dsl = DSL.using(Objects.requireNonNull(url),
                Objects.requireNonNull(username),
                password);

        this.config = Objects.requireNonNull(config);
        this.auth = new AccountQueriesImpl(dsl, config);
        this.server = new ServerQueriesImpl(dsl);
        this.punishment = new PunishmentQueriesImpl(dsl, auth);
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
}
