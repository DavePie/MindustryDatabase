package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.AccountQueries.*;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static net.ddns.mindustry.database.client.AccountQueries.SignupStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class AuthenticationQueriesTest {

    private static Database db;

    @BeforeAll
    static void initialize() {
        db = DbInitialization.clearAndConnect(2);
    }

    @AfterAll
    static void close() {
        if (db != null) db.close();
    }

    @Test
    @Order(1)
    void signup() {

        final var mock = MockAccount.instance();
        final AccountQueries.SignupStatus status = db.account().signup(
                mock.username(), mock.password(),
                mock.ip(), mock.uuid(), Duration.ofHours(1));

        assertInstanceOf(Created.class, status, "The account did not get created: " + status);
    }

    @Test
    @Order(2)
    void logout() {
        final var mock = MockAccount.instance();
        final Account account = db.account().find(mock.username()).orElse(null);
        assertNotNull(account, "The account is not available.");
        db.account().logout(account);
    }

    @Test
    @Order(3)
    void login() {

        final var mock = MockAccount.instance();
        final AccountQueries.LoginStatus login = db.account()
                .login(mock.username(), mock.password(), mock.ip(), mock.uuid(), Duration.ofHours(1));

        assertInstanceOf(LoginStatus.LoggedIn.class, login, "Could not login into the account: " + login);
    }

    @Test
    void signupLimit() {

        final var mock = MockAccount.instance();
        final int accountLimit = db.securityConfig().accountLimit();
        final String ip = "10.10.40.3";

        for (int i = 0; i < (accountLimit + 1); i++) {

            final AccountQueries.SignupStatus status = db.account().signup(
                    mock.username() + "_" + i,
                    mock.password(), ip, mock.uuid(), Duration.ofHours(1));

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

        final String ip = "12.0.0.";
        final var mock = MockAccount.instance();
        // I do this because the password is cleared and deleted.
        final Supplier<char[]> password = () -> Arrays.copyOf(mock.password(), mock.password().length);
        final Duration session = Duration.ofHours(1);

        // I create the test accounts.
        final Account[] accounts = new Account[7];
        // Custom DB instance to overwrite and increase the account limit.
        try (var db = DbInitialization.newConnection(100)) {
            for (int i = 0; i < 7; i++) {
                final Account account = ((Created) db.account().signup(
                        "limit_check_" + i, password.get(),
                        ip + i,
                        mock.uuid(), session)).account();
                db.account().logout(account);
                accounts[i] = account;
            }
        }
        // The last 3 accounts are innocent, those will be used to verify if unlinked accounts are also retrieved.
        /*
        account 0: 127.0.0.0, 127.0.0.1
        account 1: 127.0.0.1, 127.0.0.3
        account 2: 127.0.0.2, 127.0.0.0
        account 3: 127.0.0.3
         */
        // I connect account 0 with account 1 via address 127.0.0.1
        assertInstanceOf(LoginStatus.LoggedIn.class, db.account().login(accounts[0].username(), password.get(), ip + "1", mock.uuid(), session));
        // I connect account 2 with account 0 via address 127.0.0.2
        assertInstanceOf(LoginStatus.LoggedIn.class, db.account().login(accounts[2].username(), password.get(), ip + "0", mock.uuid(), session));
        // I connect account 1 with account 3 via address 127.0.0.3
        assertInstanceOf(LoginStatus.LoggedIn.class, db.account().login(accounts[1].username(), password.get(), ip + "3", mock.uuid(), session));
        // I don't check the account directly since the equality is done over object identity, which is different.
        final var tracked = db.account().findAccounts(ip + "3")
                .stream()
                .map(Account::id)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(4, tracked.size());
        assertTrue(tracked.contains(accounts[0].id()));
        assertTrue(tracked.contains(accounts[1].id()));
        assertTrue(tracked.contains(accounts[2].id()));
        assertTrue(tracked.contains(accounts[3].id()));
    }
}
