package net.ddns.mindustry.database.testclient.tests;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Role;
import net.ddns.mindustry.database.testclient.DbInitialization;
import net.ddns.mindustry.database.testclient.data.MockAccount;
import net.ddns.mindustry.database.testclient.util.AccountUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.util.Set;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public final class RoleTest {

    private static final String ROLE1 = "Role1";
    private static final String ROLE2 = "Role2";
    private final Database db;
    private final Account account;

    public RoleTest() {
        this.db = DbInitialization.newConnection(2);
        this.account = AccountUtil.signupAssertive(db, MockAccount.random());
    }

    @Test
    public void addRole() {

        final var role = db.role().newRole(ROLE1, "ffffff", "", (short) 1).orElse(null);
        Assertions.assertNotNull(role);
        Assertions.assertEquals(ROLE1, role.name());

        final var foundRole = db.role().findRole(ROLE1).orElse(null);
        Assertions.assertNotNull(foundRole);
        Assertions.assertEquals(ROLE1, foundRole.name());

        // Should be null, since the role has already been added.
        final var roleConflict = db.role().newRole(ROLE1, "ffffff", "", (short) 1).orElse(null);
        Assertions.assertNull(roleConflict);
    }

    @Test
    public void accountRoles() {

        final var role = db.role().newRole(ROLE2, "ffffff", "", (short) 1).orElseThrow();
        final int granted = db.role().grantRoles(account, role);
        Assertions.assertEquals(1, granted);

        final Set<String> roles = db.role().accountRoles(account)
                .stream()
                .map(Role::name)
                .collect(Collectors.toSet());
        Assertions.assertTrue(roles.contains(ROLE2));
    }
}
