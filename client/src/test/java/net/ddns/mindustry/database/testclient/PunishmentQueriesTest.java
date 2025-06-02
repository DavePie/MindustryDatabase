package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.DatabaseEvents;
import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.*;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.data.MockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
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

        db = DbInitialization.clearAndConnect(2);

        // I initialize the necessary things to run this test.

        final var account = MockAccount.instance();
        final var server  = MockServer.instance();

        Assertions.assertInstanceOf(
                AccountQueries.SignupStatus.Created.class,
                db.account().signup(USER_PUNISHED, account.password(), account.ip(), account.uuid(), Duration.ofHours(1)));

        Assertions.assertInstanceOf(
                AccountQueries.SignupStatus.Created.class,
                db.account().signup(USER_STAFF, account.password(), account.ip(), account.uuid(), Duration.ofHours(1)));

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
        db.events().register(DatabaseEvents.Type.BAN, listener);

        final Account punished = db.account().find(USER_PUNISHED).orElseThrow();
        final PunishmentQueries.Issuer punisher = db.account()
                .find(USER_STAFF)
                .map(PunishmentQueries.Issuer::of)
                .orElseThrow();

        final Ban ban = db.punishment().ban(punished, punisher, reason, MockServer.fromDb(db), Duration.ofDays(15));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(ban.id(), id);
        });
        db.events().unregister(DatabaseEvents.Type.BAN, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void kick(String reason) {

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        final IntConsumer listener = future::complete;
        db.events().register(DatabaseEvents.Type.KICK, listener);

        final Account punished = db.account().find(USER_PUNISHED).orElseThrow();
        final PunishmentQueries.Issuer punisher = db.account()
                .find(USER_STAFF)
                .map(PunishmentQueries.Issuer::of)
                .orElseThrow();

        final Kick kick = db.punishment().kick(punished, punisher, reason, MockServer.fromDb(db));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(kick.id(), id);
        });
        db.events().unregister(DatabaseEvents.Type.KICK, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void warn(String reason) {

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        final IntConsumer listener = future::complete;
        db.events().register(DatabaseEvents.Type.WARN, listener);

        final Account punished = db.account().find(USER_PUNISHED).orElseThrow();
        final PunishmentQueries.Issuer punisher = db.account()
                .find(USER_STAFF)
                .map(PunishmentQueries.Issuer::of)
                .orElseThrow();

        final Warn warn = db.punishment().warn(punished, punisher, reason, MockServer.fromDb(db));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(warn.id(), id);
        });
        db.events().unregister(DatabaseEvents.Type.WARN, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void mute(String reason) {

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        final IntConsumer listener = future::complete;
        db.events().register(DatabaseEvents.Type.MUTE, listener);

        final Account punished = db.account().find(USER_PUNISHED).orElseThrow();
        final PunishmentQueries.Issuer punisher = db.account()
                .find(USER_STAFF)
                .map(PunishmentQueries.Issuer::of)
                .orElseThrow();

        final Mute mute = db.punishment().mute(punished, punisher, reason, MockServer.fromDb(db), Duration.of(5, ChronoUnit.DAYS));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            final int id = future.get();
            Assertions.assertEquals(mute.id(), id);
        });
        db.events().unregister(DatabaseEvents.Type.MUTE, listener);
    }
}
