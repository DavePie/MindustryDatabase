package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import org.junit.jupiter.api.*;
import java.time.Duration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class AuthenticationQueriesTest {

    private static Database db;

    @BeforeAll
    static void initialize() {
        db = DbInitialization.prepareDatabase();
    }

    @AfterAll
    static void close() {
        if (db != null) db.close();
    }

    @Test
    @Order(1)
    void signup() {

        final var mock = MockAccount.instance();
        final AccountQueries.SignupStatus status = db.auth()
                .signup(mock.username(), mock.password(), mock.ip(), mock.uuid(), Duration.ofHours(1), 2);

        if (!(status instanceof AccountQueries.SignupStatus.Created(var account))) {
            Assertions.fail("The account did not get created: " + status);
            return;
        }
        Assertions.assertNotNull(account);
    }

    @Test
    @Order(2)
    void logout() {
        final var mock = MockAccount.instance();
        final Account account = db.auth().find(mock.username()).orElse(null);
        if (account == null) {
            Assertions.fail("The account is not available");
            return;
        }
        db.auth().logout(account);
    }

    @Test
    @Order(3)
    void login() {

        final var mock = MockAccount.instance();
        final AccountQueries.LoginStatus login = db.auth()
                .login(mock.username(), mock.password(), mock.ip(), mock.uuid(), Duration.ofHours(1));

        if (!(login instanceof AccountQueries.LoginStatus.LoggedIn(var account))) {
            Assertions.fail("Could not login into the account: " + login);
            return;
        }
        Assertions.assertNotNull(account);
    }

    @Test
    void signupLimit() {

        final var mock = MockAccount.instance();
        final int accountLimit = 2;
        final String ip = "10.10.40.3";

        for (int i = 0; i < (accountLimit + 1); i++) {

            final AccountQueries.SignupStatus status = db.auth()
                    .signup(mock.username() + "_" + i, mock.password(), ip, mock.uuid(), Duration.ofHours(1), accountLimit);

            if (i == accountLimit) {
                if (!(status instanceof AccountQueries.SignupStatus.LimitReached(int limit))) {
                    Assertions.fail("The account limit was not respected. " + status);
                    continue;
                }
                Assertions.assertEquals(accountLimit, limit);
                continue;
            }

            if (!(status instanceof AccountQueries.SignupStatus.Created(var account))) {
                Assertions.fail("The account did not get created: " + status);
                continue;
            }
            Assertions.assertNotNull(account);
        }
    }
}
