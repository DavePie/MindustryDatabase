package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.PunishmentListeners;
import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.data.MockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

public final class PunishmentQueriesTest {

    private static final String USER_PUNISHED = "punished";
    private static final String USER_STAFF    = "staff";
    private static Database db;

    @BeforeAll
    static void initialize() {

        db = DbInitialization.prepareDatabase();

        // I initialize the necessary things to run this test.

        final var account = MockAccount.instance();
        final var server  = MockServer.instance();

        Assertions.assertInstanceOf(
                AccountQueries.SignupStatus.Created.class,
                db.auth().signup(USER_PUNISHED, account.password(), account.ip(), account.uuid()));

        Assertions.assertInstanceOf(
                AccountQueries.SignupStatus.Created.class,
                db.auth().signup(USER_STAFF, account.password(), account.ip(), account.uuid()));

        db.server().add(server.ip(), server.port(), server.name());
    }

    @AfterAll
    static void close() {
        if (db != null) db.close();
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void ban(String reason) {

        final var server  = MockServer.instance();
        final var oServer = db.server().find(server.ip(), server.port());
        if (oServer.isEmpty()) Assertions.fail("The server did not get added.");

        final PunishmentQueries.Status status = db.punishment().ban(USER_PUNISHED, USER_STAFF, reason, oServer.orElseThrow(), null);
        if (status != PunishmentQueries.Status.OK) Assertions.fail("Could not add the ban: " + status);
    }

    @Test
    void banListener() {

        final var server  = MockServer.instance();
        final var oServer = db.server().find(server.ip(), server.port());
        if (oServer.isEmpty()) Assertions.fail("The server did not get added.");

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        db.punishmentListeners().register(PunishmentListeners.Type.BAN, future::complete);

        final String reason = "random_stuff";
        final PunishmentQueries.Status status = db.punishment().ban(USER_PUNISHED, USER_STAFF, reason, oServer.orElseThrow(), null);
        if (status != PunishmentQueries.Status.OK) Assertions.fail("Could not add the ban: " + status);

        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            future.get();
        });
    }
}

