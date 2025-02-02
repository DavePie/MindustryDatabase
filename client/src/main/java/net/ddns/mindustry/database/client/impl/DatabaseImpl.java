package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.SecurityConfig;
import net.ddns.mindustry.database.client.ServerQueries;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import java.util.Objects;

public final class DatabaseImpl implements Database {

    private final SecurityConfig config;
    private final AccountQueries auth;
    private final ServerQueries server;

    public DatabaseImpl(String url, String username, String password, SecurityConfig config) {

        final DSLContext dsl = DSL.using(Objects.requireNonNull(url),
                Objects.requireNonNull(username),
                password);

        this.config = Objects.requireNonNull(config);
        this.auth = new AccountQueriesImpl(dsl, config);
        this.server = new ServerQueriesImpl(dsl);
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
}
