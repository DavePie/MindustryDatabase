package net.ddns.mindustry.database.testclient.test;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.util.AccountUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static net.ddns.mindustry.database.client.AppealQueries.Status.Ok;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public final class AppealTest {

    private final Database db;

    public AppealTest() {
        this.db = DbInitialization.newConnection(2);
    }

    @AfterAll
    void close() throws Exception {
        db.close();
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#appeals")
    void newAppeal(String message) {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        // Account Appeals
        Assertions.assertTrue(db.appeal().accountAppeals(user).isEmpty());
        Assertions.assertEquals(0, db.appeal().accountAppealsCount(user));
        // Open Account Appeals
        Assertions.assertTrue(db.appeal().openAccountAppeals(user).isEmpty());
        Assertions.assertEquals(0, db.appeal().openAccountAppealsCount(user));

        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, message)).appeal();
        // Account Appeals
        Assertions.assertEquals(appeal.id(), db.appeal().accountAppeals(user).getFirst().id());
        Assertions.assertEquals(1, db.appeal().accountAppealsCount(user));
        // Open Account Appeals
        Assertions.assertEquals(appeal.id(), db.appeal().openAccountAppeals(user).getFirst().id());
        Assertions.assertEquals(1, db.appeal().openAccountAppealsCount(user));
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#appeals")
    void newAppealReply(String message) {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        final var staff = AccountUtil.signupAssertive(db, MockAccount.random());
        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "Some random appeal message.")).appeal();

        // Multiple replies are accepted, in case one staff changes idea after a while or for other reasons.
        db.appeal().replyToAppeal(appeal, staff, message, false);
        db.appeal().replyToAppeal(appeal, staff, message, true);
        // I make sure the reply closes the appeal.
        Assertions.assertTrue(db.appeal().openAccountAppeals(user).isEmpty());
        Assertions.assertEquals(0, db.appeal().openAccountAppealsCount(user));
    }
}
