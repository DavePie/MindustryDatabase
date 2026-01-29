package net.ddns.mindustry.database.testclient.tests;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public final class ConfigTest {

    private final Database db;
    private final Server server;

    public ConfigTest() {
        this.db = DbInitialization.newConnection(2);
        final var server = MockServer.instance();
        db.server().add(server.ip(), server.port(), server.name());
        this.server = MockServer.fromDb(db);
    }

    private static String makeJson(int iteration) {
        return String.format("{\"json_key\": %d}", iteration);
    }

    @RepeatedTest(1000)
    public void putAndGet(RepetitionInfo info) {

        final int index = info.getCurrentRepetition();
        final var key = index + "_" + UUID.randomUUID();
        final var json = makeJson(index);

        db.config().put(key, json);
        assertEquals(json, db.config().get(key));
    }

    @RepeatedTest(1000)
    public void putAndGetServer(RepetitionInfo info) {

        final int index = info.getCurrentRepetition();
        final var key = "server_" + index + "_" + UUID.randomUUID();
        final var json = makeJson(index);

        db.config().put(key, server, json);
        assertEquals(json, db.config().get(key, server));
    }

    @Test
    public void putConflict() {

        final var key = "conflict_" + UUID.randomUUID();
        final var unrelated = "unrelated_" + key;

        final var json0 = makeJson(0);
        final var json1 = makeJson(1);
        final var json2 = makeJson(2);

        db.config().put(unrelated, json0); // I put a random key to make sure only the key I want gets modified.

        // I put the value to be overwritten.
        db.config().put(key, json1);
        assertEquals(json1, db.config().get(key));

        // I check if no conflict happens and the key gets overwritten.
        db.config().put(key, json2);
        assertEquals(json2, db.config().get(key));

        assertEquals(json0, db.config().get(unrelated)); // I make sure the data did not change.
    }

    @Test
    public void putConflictServer() {

        final var key = "conflict_server_" + UUID.randomUUID();
        final var unrelated = "unrelated_" + key;

        final var json0 = makeJson(0);
        final var json1 = makeJson(1);
        final var json2 = makeJson(2);

        db.config().put(unrelated, server, json0); // I put a random key to make sure only the key I want gets modified.

        // I put the value to be overwritten.
        db.config().put(key, server, json1);
        assertEquals(json1, db.config().get(key, server));

        // I check if no conflict happens and the key gets overwritten.
        db.config().put(key, server, json2);
        assertEquals(json2, db.config().get(key, server));

        assertEquals(json0, db.config().get(unrelated, server)); // I make sure the data did not change.
    }
}
