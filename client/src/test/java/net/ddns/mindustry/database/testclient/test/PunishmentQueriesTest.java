package net.ddns.mindustry.database.testclient.test;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.DatabaseEvents;
import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.data.MockServer;
import net.ddns.mindustry.database.testclient.util.AccountUtil;
import net.ddns.mindustry.database.testclient.util.EventCycle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import static net.ddns.mindustry.database.client.AccountQueries.SignupStatus.Created;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public final class PunishmentQueriesTest {

    private final Duration eventTimeout = Duration.of(5, ChronoUnit.SECONDS);
    private final Database db;
    private final Account punished;
    private final PunishmentQueries.Issuer staff;

    public PunishmentQueriesTest() {

        // Initializing the necessary things to run the tests.

        this.db = DbInitialization.newConnection(2);
        final var mock1  = MockAccount.random();
        final var mock2  = MockAccount.random();
        final var server = MockServer.instance();

        this.punished = Assertions.assertInstanceOf(Created.class, AccountUtil.signup(db, mock1)).account();
        final Account staff = Assertions.assertInstanceOf(Created.class, AccountUtil.signup(db, mock2)).account();
        this.staff = PunishmentQueries.Issuer.of(staff);
        db.server().add(server.ip(), server.port(), server.name());
    }

    @AfterAll
    void close() throws Exception {
        db.close();
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#reasons")
    void ban(String reason) {
        EventCycle.verify(db,
                DatabaseEvents.Type.BAN,
                () -> db.punishment().ban(punished, staff, reason, MockServer.fromDb(db), Duration.ofDays(15)).id(),
                eventTimeout);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#reasons")
    void kick(String reason) {
        EventCycle.verify(db,
                DatabaseEvents.Type.KICK,
                () -> db.punishment().kick(punished, staff, reason, MockServer.fromDb(db)).id(),
                eventTimeout);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#reasons")
    void warn(String reason) {
        EventCycle.verify(db,
                DatabaseEvents.Type.WARN,
                () -> db.punishment().warn(punished, staff, reason, MockServer.fromDb(db)).id(),
                eventTimeout);
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#reasons")
    void mute(String reason) {
        EventCycle.verify(db,
                DatabaseEvents.Type.MUTE,
                () -> db.punishment().mute(punished, staff, reason, MockServer.fromDb(db), Duration.of(5, ChronoUnit.DAYS)).id(),
                eventTimeout);
    }

    @RepeatedTest(50)
    void banStress() {
        ban("reason");
    }

    @RepeatedTest(50)
    void kickStress() {
        kick("reason");
    }

    @RepeatedTest(50)
    void warnStress() {
        warn("reason");
    }

    @RepeatedTest(50)
    void muteStress() {
        mute("reason");
    }
}
