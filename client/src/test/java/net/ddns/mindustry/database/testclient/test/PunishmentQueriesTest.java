package net.ddns.mindustry.database.testclient.test;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.DatabaseEvents;
import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.*;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.data.MockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntConsumer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public final class PunishmentQueriesTest {

    private final Database db;
    private final Account punished;
    private final Account staff;

    public PunishmentQueriesTest() {

        // Initializing the necessary things to run the tests.

        this.db = DbInitialization.newConnection(2);
        final var mock1  = MockAccount.random();
        final var mock2  = MockAccount.random();
        final var server = MockServer.instance();

        this.punished = Assertions.assertInstanceOf(AccountQueries.SignupStatus.Created.class, db.account().signup(
                mock1.username(),
                mock1.password(),
                mock1.ip(),
                mock1.uuid(),
                Duration.ofHours(1))).account();
        this.staff = Assertions.assertInstanceOf(AccountQueries.SignupStatus.Created.class, db.account().signup(
                mock2.username(),
                mock2.password(),
                mock2.ip(),
                mock2.uuid(),
                Duration.ofHours(1))).account();
        db.server().add(server.ip(), server.port(), server.name());
    }

    @AfterAll
    void close() throws Exception {
        db.close();
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void ban(String reason) {

        final var queue = new LinkedBlockingQueue<Integer>();
        final IntConsumer listener = queue::add;
        db.events().register(DatabaseEvents.Type.BAN, listener);
        final Ban ban = db.punishment().ban(punished, PunishmentQueries.Issuer.of(staff), reason, MockServer.fromDb(db), Duration.ofDays(15));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            while (true) {
                final int id = queue.take();
                if (id == ban.id()) break;
            }
        });
        db.events().unregister(DatabaseEvents.Type.BAN, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void kick(String reason) {

        final var queue = new LinkedBlockingQueue<Integer>();
        final IntConsumer listener = queue::add;
        db.events().register(DatabaseEvents.Type.KICK, listener);

        final Kick kick = db.punishment().kick(punished, PunishmentQueries.Issuer.of(staff), reason, MockServer.fromDb(db));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            while (true) {
                final int id = queue.take();
                if (id == kick.id()) break;
            }
        });
        db.events().unregister(DatabaseEvents.Type.KICK, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void warn(String reason) {

        final var queue = new LinkedBlockingQueue<Integer>();
        final IntConsumer listener = queue::add;
        db.events().register(DatabaseEvents.Type.WARN, listener);

        final Warn warn = db.punishment().warn(punished, PunishmentQueries.Issuer.of(staff), reason, MockServer.fromDb(db));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            while (true) {
                final int id = queue.take();
                if (id == warn.id()) break;
            }
        });
        db.events().unregister(DatabaseEvents.Type.WARN, listener);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockPunishment#reasons")
    void mute(String reason) {

        final var queue = new LinkedBlockingQueue<Integer>();
        final IntConsumer listener = queue::add;
        db.events().register(DatabaseEvents.Type.MUTE, listener);

        final Mute mute = db.punishment().mute(punished, PunishmentQueries.Issuer.of(staff), reason, MockServer.fromDb(db), Duration.of(5, ChronoUnit.DAYS));
        Assertions.assertTimeoutPreemptively(Duration.of(5, ChronoUnit.SECONDS), () -> {
            while (true) {
                final int id = queue.take();
                if (id == mute.id()) break;
            }
        });
        db.events().unregister(DatabaseEvents.Type.MUTE, listener);
    }
}
