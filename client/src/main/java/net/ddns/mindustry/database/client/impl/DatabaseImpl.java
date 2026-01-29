package net.ddns.mindustry.database.client.impl;

import com.zaxxer.hikari.HikariDataSource;
import net.ddns.mindustry.database.client.*;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Inet;
import org.postgresql.Driver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DatabaseImpl implements Database {

    private final HikariDataSource ds;
    private final DSLContext dsl;
    private final SecurityConfig securityConfig;
    private final AccountQueriesImpl auth;
    private final ServerQueriesImpl server;
    private final ServerAccountQueriesImpl serverAccount;
    private final PunishmentQueriesImpl punishment;
    private final RoleQueriesImpl role;
    private final AppealQueriesImpl appeal;
    private final PunishmentListenerImpl databaseEvents;
    private final ConfigQueriesImpl config;

    public DatabaseImpl(String url, String username, String password, SecurityConfig config) {
        this.ds = createDataSource(url, username, password);
        this.dsl = DSL.using(ds, SQLDialect.POSTGRES);
        this.securityConfig = Objects.requireNonNull(config);
        this.auth = new AccountQueriesImpl(this);
        this.server = new ServerQueriesImpl(this);
        this.serverAccount = new ServerAccountQueriesImpl(this);
        this.punishment = new PunishmentQueriesImpl(this);
        this.role = new RoleQueriesImpl(this);
        this.appeal = new AppealQueriesImpl(this);
        this.config = new ConfigQueriesImpl(this);
        // Must be last since it uses the classes above during initialization.
        this.databaseEvents = new PunishmentListenerImpl(this);
    }

    private static HikariDataSource createDataSource(String url, String username, String password) {
        final var ds = new HikariDataSource();
        ds.setDriverClassName(Driver.class.getName());
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        // Avoids a problem where the pool could end up with zero connections.
        ds.addDataSourceProperty("tcpKeepAlive", "true");
        return ds;
    }

    public static Inet inet(String ip) throws IllegalArgumentException {
        Objects.requireNonNull(ip);
        try { return Inet.inet(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("The provided IP address is invalid.", e);
        }
    }

    public static boolean isIntWithinLong(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    public String sqidsEncode(int id) {
        return securityConfig()
                .sqids()
                .encode(List.of((long) id));
    }

    /// @return the integer version of the uid, but in case the uid does not fit inside an int, empty is returned.
    public Optional<Integer> sqidsDecodeInt(String uid) {
        Objects.requireNonNull(uid);
        final var ids = securityConfig()
                .sqids()
                .decode(uid);
        // The id used is an integer, will never take multiple longs to find.
        if (ids.size() != 1 && DatabaseImpl.isIntWithinLong(ids.getFirst())) return Optional.empty();
        return Optional.of(Math.toIntExact(ids.getFirst()));
    }

    public DSLContext dsl() {
        return dsl;
    }

    @Override
    public SecurityConfig securityConfig() {
        return securityConfig;
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
    public PunishmentListenerImpl listeners() {
        return databaseEvents;
    }

    @Override
    public ConfigQueries config() {
        return config;
    }

    @Override
    public void close() {
        ds.close();
        databaseEvents.close();
    }
}
