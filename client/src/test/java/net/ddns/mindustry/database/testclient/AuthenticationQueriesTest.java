package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import org.junit.jupiter.api.*;

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
                .signup(mock.username(), mock.password(), mock.ip(), mock.uuid());

        if (!(status instanceof AccountQueries.SignupStatus.Created(var account))) {
            Assertions.fail("The account did not get created: " + status);
            return;
        }
        Assertions.assertNotNull(account);
    }

    @Test
    void login() {

        final var mock = MockAccount.instance();
        final AccountQueries.LoginStatus login = db.auth()
                .login(mock.username(), mock.password(), mock.ip(), mock.uuid(), 1);

        if (!(login instanceof AccountQueries.LoginStatus.LoggedIn(var account))) {
            Assertions.fail("Could not login into the account.");
            return;
        }
        Assertions.assertNotNull(account);
    }
}
