package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.ConfigQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.*;
import org.jspecify.annotations.Nullable;
import java.util.Objects;
import static net.ddns.mindustry.database.schema.Tables.CONFIGURATION;

public record ConfigQueriesImpl(DatabaseImpl database) implements ConfigQueries {

    @Override
    public void put(String key, String value) {

        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        final var json = JSONB.valueOf(value);

        database.dsl().transaction(connection -> {
            final DSLContext tDsl = connection.dsl();
            tDsl.insertInto(CONFIGURATION)
                    .set(CONFIGURATION.KEY, key)
                    .set(CONFIGURATION.VALUE, json)
                    .onConflict(CONFIGURATION.KEY, CONFIGURATION.SERVER_ID)
                    .doUpdate()
                    .set(CONFIGURATION.VALUE, json)
                    .execute();
        });
    }

    @Override
    public void put(String key, Server server, String value) {

        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        Objects.requireNonNull(server);

        final var json = JSONB.valueOf(value);

        database.dsl().transaction(connection -> {
            final DSLContext tDsl = connection.dsl();
            tDsl.insertInto(CONFIGURATION)
                    .set(CONFIGURATION.KEY, key)
                    .set(CONFIGURATION.VALUE, json)
                    .set(CONFIGURATION.SERVER_ID, server.id())
                    .onConflict(CONFIGURATION.KEY, CONFIGURATION.SERVER_ID)
                    .doUpdate()
                    .set(CONFIGURATION.VALUE, json)
                    .execute();
        });
    }

    @Override
    public @Nullable String get(String key) {
        Objects.requireNonNull(key);
        return database.dsl().<@Nullable String>transactionResult(connection -> {
            final DSLContext tDsl = connection.dsl();
            return tDsl.select(CONFIGURATION.VALUE)
                    .from(CONFIGURATION)
                    .where(CONFIGURATION.KEY.eq(key))
                    .fetchOptional()
                    .map(Record1::component1)
                    .map(JSONB::data)
                    .orElse(null);
        });
    }

    @Override
    public @Nullable String get(String key, Server server) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(server);
        return database.dsl().<@Nullable String>transactionResult(connection -> {
            final DSLContext tDsl = connection.dsl();
            return tDsl.select(CONFIGURATION.VALUE)
                    .from(CONFIGURATION)
                    .where(CONFIGURATION.KEY.eq(key).and(CONFIGURATION.SERVER_ID.eq(server.id())))
                    .fetchOptional()
                    .map(Record1::component1)
                    .map(JSONB::data)
                    .orElse(null);
        });
    }
}
