package net.ddns.mindustry.database.testclient.tests;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Permission;
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

    private final Database db;
    private final Account account;

    public RoleTest() {
        this.db = DbInitialization.newConnection();
        this.account = AccountUtil.signupAssertive(db, MockAccount.random());
    }

    @Test
    public void addRole() {

        final String roleName = "AddRole";

        final var role = db.role().newRole(roleName, "ffffff", "", (short) 1).orElse(null);
        Assertions.assertNotNull(role);
        Assertions.assertEquals(roleName, role.name());

        final var foundRole = db.role().findRole(roleName).orElse(null);
        Assertions.assertNotNull(foundRole);
        Assertions.assertEquals(roleName, foundRole.name());

        // Should be null, since the role has already been added.
        final var roleConflict = db.role().newRole(roleName, "ffffff", "", (short) 1).orElse(null);
        Assertions.assertNull(roleConflict);
    }

    @Test
    public void addPermission() {

        final String roleName = "AddPermission";
        final String permProp = "AddPermission";

        final var role = db.role().newRole(roleName, "ffffff", "", (short) 1).orElseThrow();
        final var perm = db.role().newPermission(permProp).orElse(null);
        Assertions.assertNotNull(perm);

        // Should be null, since the role has already been added.
        final var permConflict = db.role().newPermission(permProp).orElse(null);
        Assertions.assertNull(permConflict);

        final int linked = db.role().linkPermissions(role, perm);
        Assertions.assertEquals(1, linked);

        // The permission has already been linked.
        final int linkedConflict = db.role().linkPermissions(role, perm);
        Assertions.assertEquals(0, linkedConflict);
    }

    @Test
    public void accountRoles() {

        final String roleName = "AccountRoles";

        final var role = db.role().newRole(roleName, "ffffff", "", (short) 1).orElseThrow();
        final int granted = db.role().grantRoles(account, role);
        Assertions.assertEquals(1, granted);

        final int grantedConflict = db.role().grantRoles(account, role);
        Assertions.assertEquals(0, grantedConflict);

        final Set<String> roles = db.role().accountRoles(account)
                .stream()
                .map(Role::name)
                .collect(Collectors.toSet());
        Assertions.assertTrue(roles.contains(roleName));
    }

    @Test
    public void rolePermissions() {

        final String roleName = "RolePermission";
        final String permProp = "RolePermission";

        final var role = db.role().newRole(roleName, "ffffff", "", (short) 1).orElseThrow();
        final var perm = db.role().newPermission(permProp).orElseThrow();
        db.role().linkPermissions(role, perm);

        final Set<String> perms = db.role().rolePermissions(role)
                .stream()
                .map(Permission::property)
                .collect(Collectors.toSet());
        Assertions.assertTrue(perms.contains(permProp));
    }
}
