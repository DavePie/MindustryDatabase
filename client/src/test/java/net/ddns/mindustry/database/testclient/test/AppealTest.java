package net.ddns.mindustry.database.testclient.test;

import net.ddns.mindustry.database.client.AppealQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.util.AccountUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static net.ddns.mindustry.database.client.AppealQueries.Status.Ok;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD) // Some tests might influence the others.
public final class AppealTest {

    private final Database db;

    public AppealTest() {
        this.db = DbInitialization.newConnection(2);
    }

    @AfterAll
    void close() throws Exception {
        db.close();
    }

    @Order(0)
    @Test
    void newAppeal() {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());

        // ======= PRE APPEAL =======
        // Empty Open Appeals
        Assertions.assertTrue(db.appeal().openAppeals(Integer.MAX_VALUE).isEmpty());
        Assertions.assertEquals(0, db.appeal().openAppealsCount());
        // Empty Account Appeals
        Assertions.assertTrue(db.appeal().accountAppeals(user).isEmpty());
        Assertions.assertEquals(0, db.appeal().accountAppealsCount(user));
        // Empty Open Account Appeals
        Assertions.assertTrue(db.appeal().openAccountAppeals(user).isEmpty());
        Assertions.assertEquals(0, db.appeal().openAccountAppealsCount(user));

        // I issue the appeal
        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "some_message")).appeal();

        // ======= POST APPEAL =======
        // Open Appeals
        Assertions.assertEquals(appeal.id(), db.appeal().openAppeals(Integer.MAX_VALUE).getFirst().id());
        Assertions.assertEquals(1, db.appeal().openAppealsCount());
        // Account Appeals
        Assertions.assertEquals(appeal.id(), db.appeal().accountAppeals(user).getFirst().id());
        Assertions.assertEquals(1, db.appeal().accountAppealsCount(user));
        // Open Account Appeals
        Assertions.assertEquals(appeal.id(), db.appeal().openAccountAppeals(user).getFirst().id());
        Assertions.assertEquals(1, db.appeal().openAccountAppealsCount(user));
    }

    @Test
    @Order(1)
    void newAppealReply() {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        final var staff = AccountUtil.signupAssertive(db, MockAccount.random());
        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "Some random appeal message.")).appeal();

        // Multiple replies are accepted, in case one staff changes idea after a while or for other reasons.
        final var appealReply1 = db.appeal().replyToAppeal(appeal, staff, "message_1", false);
        final var appealReply2 = db.appeal().replyToAppeal(appeal, staff, "message_2", true);
        // I make sure the reply closes the appeal.
        Assertions.assertTrue(db.appeal().openAccountAppeals(user).isEmpty());
        Assertions.assertEquals(0, db.appeal().openAccountAppealsCount(user));

        final var replies = db.appeal().appealReplies(appeal);
        Assertions.assertTrue(replies.contains(appealReply1));
        Assertions.assertTrue(replies.contains(appealReply2));
        Assertions.assertEquals(2, db.appeal().appealRepliesCount(appeal));
    }

    @ParameterizedTest
    @MethodSource("net.ddns.mindustry.database.testclient.data.MockMessages#appeals")
    void newAppeal(String message) {
        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, message));
    }

    @Test
    void appealRateLimit() {
        final String message = "Some random message";
        final var user = AccountUtil.signupAssertive(db, MockAccount.random());
        Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, message));
        Assertions.assertInstanceOf(AppealQueries.Status.RateLimited.class, db.appeal().appeal(user, message));
   }

   @Test
   void appealEmptyMessage() {
       final var user = AccountUtil.signupAssertive(db, MockAccount.random());
       Assertions.assertInstanceOf(AppealQueries.Status.EmptyMessage.class, db.appeal().appeal(user, "      "));
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

    @Test
    void deleteAppeal() {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "Some random appeal message.")).appeal();

        Assertions.assertEquals(1, db.appeal().accountAppealsCount(user));
        db.appeal().deleteAppeal(appeal);
        Assertions.assertEquals(0, db.appeal().accountAppealsCount(user));
    }

    @Test
    void deleteAppealReply() {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        final var staff = AccountUtil.signupAssertive(db, MockAccount.random());

        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "Some random appeal message.")).appeal();
        final var appealReply = db.appeal().replyToAppeal(appeal, staff, "Some random appeal reply message.", true);

        Assertions.assertEquals(0, db.appeal().openAccountAppealsCount(user));
        db.appeal().deleteAppealReply(appealReply);
        // The appeal re-opens since there's no longer any reply.
        Assertions.assertEquals(1, db.appeal().openAccountAppealsCount(user));
    }

    @Test
    void findAppeal() {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "Some random appeal message.")).appeal();

        final String uid = db.appeal().uidFrom(appeal);
        final var foundAppeal = db.appeal()
                .findAppeal(uid)
                .orElseThrow();
        Assertions.assertEquals(appeal, foundAppeal);
    }

    @Test
    void findAppealReply() {

        final var user  = AccountUtil.signupAssertive(db, MockAccount.random());
        final var staff = AccountUtil.signupAssertive(db, MockAccount.random());

        final var appeal = Assertions.assertInstanceOf(Ok.class, db.appeal().appeal(user, "Some random appeal message.")).appeal();
        final var appealReply = db.appeal().replyToAppeal(appeal, staff, "Some random appeal reply message.", true);

        final String uid = db.appeal().uidFrom(appealReply);
        final var foundReply = db.appeal()
                .findAppealReply(uid)
                .orElseThrow();
        Assertions.assertEquals(appealReply, foundReply);
    }
}
