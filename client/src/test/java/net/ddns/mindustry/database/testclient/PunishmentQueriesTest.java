package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.PunishmentListeners;
import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Ban;
import net.ddns.mindustry.database.schema.tables.pojos.Kick;
import net.ddns.mindustry.database.schema.tables.pojos.Warn;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.data.MockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

@Execution(ExecutionMode.CONCURRENT)
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

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        final IntConsumer listener = future::complete;
        db.punishmentListeners().register(PunishmentListeners.Type.BAN, listener);

        final var status = db.punishment().ban(USER_PUNISHED, USER_STAFF, reason, MockServer.fromDb(db), (OffsetDateTime) null);
        if (!(status instanceof PunishmentQueries.Status.Ok<Ban>(Ban ban))) {
            Assertions.fail("Could not issue the ban: " + status);
            throw new IllegalStateException();
        }
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(ban.id(), id);
        });
        db.punishmentListeners().unregister(PunishmentListeners.Type.BAN, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void kick(String reason) {

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        final IntConsumer listener = future::complete;
        db.punishmentListeners().register(PunishmentListeners.Type.KICK, listener);

        final var status = db.punishment().kick(USER_PUNISHED, USER_STAFF, reason, MockServer.fromDb(db));
        if (!(status instanceof PunishmentQueries.Status.Ok<Kick>(Kick kick))) {
            Assertions.fail("Could not issue the kick: " + status);
            throw new IllegalStateException();
        }
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(kick.id(), id);
        });
        db.punishmentListeners().unregister(PunishmentListeners.Type.KICK, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void warn(String reason) {

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        final IntConsumer listener = future::complete;
        db.punishmentListeners().register(PunishmentListeners.Type.WARN, listener);

        final var status = db.punishment().warn(USER_PUNISHED, USER_STAFF, reason, MockServer.fromDb(db));
        if (!(status instanceof PunishmentQueries.Status.Ok<Warn>(Warn warn))) {
            Assertions.fail("Could not issue the warn: " + status);
            throw new IllegalStateException();
        }
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(warn.id(), id);
        });
        db.punishmentListeners().unregister(PunishmentListeners.Type.WARN, listener);
    }
}
