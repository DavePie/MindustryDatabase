package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.*;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Inet;
import org.jspecify.annotations.NullMarked;
import org.postgresql.Driver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

@NullMarked
public final class DatabaseImpl implements Database {

    private final Connection connection;
    private final DSLContext dsl;
    private final SecurityConfig config;
    private final AccountQueriesImpl auth;
    private final ServerQueriesImpl server;
    private final ServerAccountQueriesImpl serverAccount;
    private final PunishmentQueriesImpl punishment;
    private final RoleQueriesImpl role;
    private final AppealQueriesImpl appeal;
    private final DatabaseEventsImpl databaseEvents;

    public DatabaseImpl(String url, String username, String password, SecurityConfig config) {
        this.connection = newConnection(url, username, password);
        this.dsl = DSL.using(this.connection);
        this.config = Objects.requireNonNull(config);
        this.auth = new AccountQueriesImpl(this);
        this.server = new ServerQueriesImpl(this);
        this.serverAccount = new ServerAccountQueriesImpl(this);
        this.punishment = new PunishmentQueriesImpl(this);
        this.role = new RoleQueriesImpl(this);
        this.appeal = new AppealQueriesImpl(this);
        // Must be last since it uses the classes above during initialization.
        try { this.databaseEvents = new DatabaseEventsImpl(this);
        } catch (SQLException e) {
            throw new RuntimeException("Could not start the database event listener task.", e);
        }
    }

    private static Connection newConnection(String url, String username, String password) {

        Objects.requireNonNull(url);
        Objects.requireNonNull(username);

        try { Class.forName(Driver.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The database driver could not be loaded.", e);
        }

        try {
            final var con = DriverManager.getConnection(url, username, password);
            con.setAutoCommit(false); // TODO Fix code that does not use transactions.
            return con;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Could not open a connection with the database.", e);
        }
    }

    public static Inet inet(String ip) throws IllegalArgumentException {
        Objects.requireNonNull(ip);
        try { return Inet.inet(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("The provided IP address is invalid.", e);
        }
    }

    public Connection connection() {
        return connection;
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
    public AppealQueries appeal() {
        return appeal;
    }

    @Override
    public DatabaseEventsImpl events() {
        return databaseEvents;
    }

    @Override
    public void close() throws Exception {
        databaseEvents.close();
        connection.close();
    }
}
