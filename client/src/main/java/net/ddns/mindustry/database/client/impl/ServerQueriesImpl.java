package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.ServerQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import org.jooq.types.UShort;
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
}
