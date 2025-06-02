package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.ServerQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import org.jooq.postgres.extensions.types.Inet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static net.ddns.mindustry.database.client.impl.DatabaseImpl.inet;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record ServerQueriesImpl(DatabaseImpl database) implements ServerQueries {

    public Optional<Server> find(DSLContext tDsl, int id) {
        Objects.requireNonNull(tDsl);
        return tDsl.selectFrom(SERVER)
                .where(SERVER.ID.eq(id))
                .fetchOptionalInto(Server.class);
    }

    public Optional<Server> find(DSLContext tDsl, Inet ip, int port) {
        Objects.requireNonNull(tDsl);
        Objects.requireNonNull(ip);
        return tDsl.select()
                .from(SERVER)
                .where(SERVER.IP_ADDRESS.eq(ip).and(SERVER.PORT.eq(port)))
                .fetchOptionalInto(Server.class);
    }

    public boolean isWhitelistEnabled(DSLContext tDsl, int serverId) {
        return tDsl.select(SERVER.WHITELIST_ENABLED)
                .from(SERVER)
                .where(SERVER.ID.eq(serverId))
                .fetchOptionalInto(boolean.class)
                .orElse(false);
    }

    @Override
    public Optional<Server> find(int id) {
        return find(database().dsl(), id);
    }

    @Override
    public Optional<Server> find(String ip, int port) {
        final Inet inet = inet(ip);
        return find(database().dsl(), inet, port);
    }

    @Override
    public List<Server> listAll() {
        return database().dsl()
                .selectFrom(SERVER)
                .fetchInto(Server.class);
    }

    @Override
    public boolean add(String ip, int port, String name) {
        final Inet inet = inet(ip);
        return database().dsl()
                .insertInto(SERVER)
                .set(SERVER.IP_ADDRESS, inet)
                .set(SERVER.PORT, port)
                .set(SERVER.NAME, name)
                .onConflictDoNothing()
                .execute() == 1; // 1 row if the server has been added, 0 if already present.
    }

    @Override
    public void update(Server server, @Nullable String newIP, @Nullable Integer newPort, @Nullable String newName) {
        Objects.requireNonNull(server);
        database().dsl()
                .update(SERVER)
                .set(SERVER.IP_ADDRESS, newIP == null ? server.ipAddress() : inet(newIP))
                .set(SERVER.PORT, newPort == null ? server.port() : newPort)
                .set(SERVER.NAME, newName == null ? server.name() : newName)
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public void heartbeat(Server server) {
        Objects.requireNonNull(server);
        database().dsl()
                .update(SERVER)
                .set(SERVER.HEARTBEAT, OffsetDateTime.now())
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public void remove(Server server) {
        Objects.requireNonNull(server);
        database().dsl()
                .deleteFrom(SERVER)
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public void modifyWhitelist(Server server, boolean enabled) {
        Objects.requireNonNull(server);
        database().dsl()
                .update(SERVER)
                .set(SERVER.WHITELIST_ENABLED, enabled)
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public boolean isWhitelistEnabled(Server server) {
        return isWhitelistEnabled(database().dsl(), server.id());
    }
}
