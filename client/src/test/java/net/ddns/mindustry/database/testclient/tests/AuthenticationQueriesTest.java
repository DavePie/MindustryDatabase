package net.ddns.mindustry.database.testclient.tests;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.AccountQueries.*;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.util.AccountUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.time.Duration;
import java.util.stream.Collectors;
import static net.ddns.mindustry.database.client.AccountQueries.SignupStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD) // I need the execution order for this test.
public final class AuthenticationQueriesTest {

    private final Database db;
    private final MockAccount account;

    public AuthenticationQueriesTest() {
        this.db = DbInitialization.newConnection(2);
        this.account = MockAccount.random();
    }

    @AfterAll
    void close() throws Exception {
        db.close();
    }

    @Test
    @Order(1)
    void signup() {
        final AccountQueries.SignupStatus status = AccountUtil.signup(db, account);
        assertInstanceOf(Created.class, status, "The account did not get created: " + status);
    }

    @Test
    @Order(2)
    void logout() {
        final Account account = db.account().find(this.account.username()).orElse(null);
        assertNotNull(account, "The account is not available.");
        db.account().logout(account);
    }

    @Test
    @Order(3)
    void login() {

        final AccountQueries.LoginStatus login = db.account().login(
                account.username(),
                account.password().clone(),
                account.ip(),
                account.uuid(),
                Duration.ofHours(1));

        assertInstanceOf(LoginStatus.LoggedIn.class, login, "Could not login into the account: " + login);
    }

    @Test
    void signupLimit() {

        final int accountLimit = db.securityConfig().accountLimit();
        final String ip = "10.10.40.3";

        for (int i = 0; i < (accountLimit + 1); i++) {
            final var mock = MockAccount.random().withIp(ip); // Same IP to verify the limit.
            final AccountQueries.SignupStatus status = AccountUtil.signup(db, mock);
            if (i == accountLimit) {
                assertInstanceOf(LimitReached.class, status, "The account limit was not respected. " + status);
                assertEquals(accountLimit, ((LimitReached) status).limit());
                continue;
            }
            assertInstanceOf(Created.class, status, "The account did not get created: " + status);
        }
    }

    @Test
    void findAccountsByIp() {

        record Pair(MockAccount mock, Account account) {}
        final String baseIp = "12.0.0.";

        // I create the test accounts.
        final Pair[] pairs = new Pair[7];
        // Custom DB instance to overwrite and increase the account limit.
        try (var db = DbInitialization.newConnection(100)) {
            for (int i = 0; i < 7; i++) {

                final var mock    = MockAccount.random().withIp(baseIp + i);
                final var account = Assertions.assertInstanceOf(Created.class, AccountUtil.signup(db, mock)).account();
                db.account().logout(account);

                pairs[i] = new Pair(mock, account);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not close the database connection", e);
        }
        // The last 3 accounts are innocent, those will be used to verify if unlinked accounts are also retrieved.
        /*
        account 0: 127.0.0.0, 127.0.0.1
        account 1: 127.0.0.1, 127.0.0.3
        account 2: 127.0.0.2, 127.0.0.0
        account 3: 127.0.0.3
         */
        // I connect account 0 with account 1 via address 127.0.0.1
        assertInstanceOf(LoginStatus.LoggedIn.class, AccountUtil.login(db, pairs[0].mock().withIp(baseIp + "1")));
        // I connect account 2 with account 0 via address 127.0.0.2
        assertInstanceOf(LoginStatus.LoggedIn.class, AccountUtil.login(db, pairs[2].mock().withIp(baseIp + "0")));
        // I connect account 1 with account 3 via address 127.0.0.3
        assertInstanceOf(LoginStatus.LoggedIn.class, AccountUtil.login(db, pairs[1].mock().withIp(baseIp + "3")));
        // I don't check the account directly since the equality is done over object identity, which is different.
        final var tracked = db.account().findAccounts(baseIp + "3")
                .stream()
                .map(Account::id)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(4, tracked.size());
        assertTrue(tracked.contains(pairs[0].account().id()));
        assertTrue(tracked.contains(pairs[1].account().id()));
        assertTrue(tracked.contains(pairs[2].account().id()));
        assertTrue(tracked.contains(pairs[3].account().id()));
    }
}
