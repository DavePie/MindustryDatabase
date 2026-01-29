package net.ddns.mindustry.database.testclient.tests;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.util.UUID;

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
        final var key = UUID.randomUUID() + "_" + index;
        final var json = makeJson(index);
        db.config().put(key, json);
        Assertions.assertEquals(json,db.config().get(key));
    }

    @RepeatedTest(1000)
    public void putAndGetServer(RepetitionInfo info) {
        final int index = info.getCurrentRepetition();
        final var key = "server_" + UUID.randomUUID() + index;
        final var json = makeJson(index);
        db.config().put(key, server, json);
        Assertions.assertEquals(json, db.config().get(key, server));
    }
}
