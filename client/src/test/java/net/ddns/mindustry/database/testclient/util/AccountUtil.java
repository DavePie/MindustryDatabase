package net.ddns.mindustry.database.testclient.util;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import java.time.Duration;
import java.util.Objects;

public final class AccountUtil {

    public static final Duration DEFAULT = Duration.ofHours(1);

    public static AccountQueries.SignupStatus signup(Database database, MockAccount account) {
        Objects.requireNonNull(database);
        Objects.requireNonNull(account);
        return database.account().signup(
                account.username(),
                account.password().clone(), // I clone to avoid deleting the mock account password, since re-used.
                account.ip(),
                account.uuid(),
                DEFAULT);
    }

    public static AccountQueries.LoginStatus login(Database database, MockAccount account) {
        Objects.requireNonNull(database);
        Objects.requireNonNull(account);
        return database.account().login(
                account.username(),
                account.password().clone(), // I clone to avoid deleting the mock account password, since re-used.
                account.ip(),
                account.uuid(),
                DEFAULT);
    }
}
