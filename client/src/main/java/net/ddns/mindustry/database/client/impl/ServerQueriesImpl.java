package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.ServerQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import org.jooq.postgres.extensions.types.Inet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record ServerQueriesImpl(DSLContext dsl) implements ServerQueries {

    private static Inet inet(String ip) throws IllegalArgumentException {
        Objects.requireNonNull(ip);
        try { return Inet.inet(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("The provided IP address is invalid.", e);
        }
    }

    @Override
    public Optional<Server> get(int id) {
        return dsl.selectFrom(SERVER)
                .where(SERVER.ID.eq(id))
                .fetchOptionalInto(Server.class);
    }

    @Override
    public Optional<Server> find(String ip, int port) {
        final Inet inet = inet(ip);
        return dsl.selectFrom(SERVER)
                .where(SERVER.IP_ADDRESS.eq(inet).and(SERVER.PORT.eq(port)))
                .fetchOptionalInto(Server.class);
    }

    @Override
    public List<Server> getAll() {
        return dsl.selectFrom(SERVER)
                .fetchInto(Server.class);
    }

    @Override
    public void add(String ip, int port, String name) {

        if (find(ip, port).isPresent()) {
            return;
        }

        final Inet inet = inet(ip);

        dsl.insertInto(SERVER)
                .set(SERVER.IP_ADDRESS, inet)
                .set(SERVER.PORT, port)
                .set(SERVER.NAME, name)
                .execute();
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
