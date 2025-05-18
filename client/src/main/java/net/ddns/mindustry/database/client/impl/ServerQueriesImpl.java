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
public record ServerQueriesImpl(DSLContext dsl) implements ServerQueries {

    Optional<Server> find(DSLContext tDsl, int id) {
        Objects.requireNonNull(tDsl);
        return tDsl.selectFrom(SERVER)
                .where(SERVER.ID.eq(id))
                .fetchOptionalInto(Server.class);
    }

    Optional<Server> find(DSLContext tDsl, Inet ip, int port) {
        Objects.requireNonNull(tDsl);
        Objects.requireNonNull(ip);
        return dsl.select()
                .from(SERVER)
                .where(SERVER.IP_ADDRESS.eq(ip).and(SERVER.PORT.eq(port)))
                .fetchOptionalInto(Server.class);
    }

    @Override
    public Optional<Server> find(int id) {
        return find(dsl, id);
    }

    @Override
    public Optional<Server> find(String ip, int port) {
        final Inet inet = inet(ip);
        return find(dsl, inet, port);
    }

    @Override
    public List<Server> listAll() {
        return dsl.selectFrom(SERVER)
                .fetchInto(Server.class);
    }

    @Override
    public boolean add(String ip, int port, String name) {
        final Inet inet = inet(ip);
        return dsl.insertInto(SERVER)
                .set(SERVER.IP_ADDRESS, inet)
                .set(SERVER.PORT, port)
                .set(SERVER.NAME, name)
                .onConflictDoNothing()
                .execute() == 1; // 1 row if the server has been added, 0 if already present.
    }

    @Override
    public void update(Server server, @Nullable String newIP, @Nullable Integer newPort, @Nullable String newName) {
        Objects.requireNonNull(server);
        dsl.update(SERVER)
                .set(SERVER.IP_ADDRESS, newIP == null ? server.ipAddress() : inet(newIP))
                .set(SERVER.PORT, newPort == null ? server.port() : newPort)
                .set(SERVER.NAME, newName == null ? server.name() : newName)
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public void heartbeat(Server server) {
        Objects.requireNonNull(server);
        dsl.update(SERVER)
                .set(SERVER.HEARTBEAT, OffsetDateTime.now())
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public void remove(Server server) {
        Objects.requireNonNull(server);
        dsl.deleteFrom(SERVER)
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }
}
