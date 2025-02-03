package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.ServerQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import org.jooq.types.UShort;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import static net.ddns.mindustry.database.schema.Tables.*;

public record ServerQueriesImpl(DSLContext dsl) implements ServerQueries {
    @Override
    public Optional<Server> find(String ip, int port) {

        Objects.requireNonNull(ip);
        final UShort uPort = UShort.valueOf(port);

        return dsl.selectFrom(SERVER)
                .where(SERVER.IP_ADDRESS.eq(ip).and(SERVER.PORT.eq(uPort)))
                .fetchOptionalInto(Server.class);
    }

    @Override
    public void add(String ip, int port, String name) {
        Objects.requireNonNull(ip);
        final UShort uPort = UShort.valueOf(port);

        if (find(ip, port).isPresent()) {
            return;
        }

        dsl.insertInto(SERVER)
                .set(SERVER.IP_ADDRESS, ip)
                .set(SERVER.PORT, uPort)
                .set(SERVER.NAME, name)
                .execute();
    }

    @Override
    public void update(Server server, String newIP, Integer newPort, String newName) {
        Objects.requireNonNull(server);

        newIP = Objects.requireNonNullElse(newIP, server.ipAddress());
        newPort = Objects.requireNonNullElse(newPort, server.port().intValue());
        newName = Objects.requireNonNullElse(newName, server.name());

        // I'm not trusting that the valueOf method is null safe.
        UShort uNewPort = UShort.valueOf(newPort);

        dsl.update(SERVER)
                .set(SERVER.IP_ADDRESS, newIP)
                .set(SERVER.PORT, uNewPort)
                .set(SERVER.NAME, newName)
                .where(SERVER.ID.eq(server.id()))
                .execute();
    }

    @Override
    public void heartbeat(Server server) {
        Objects.requireNonNull(server);

        dsl.update(SERVER)
                .set(SERVER.HEARTBEAT, LocalDateTime.now())
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
