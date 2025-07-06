package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.RoleQueries;
import net.ddns.mindustry.database.schema.Tables;
import net.ddns.mindustry.database.schema.tables.pojos.*;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static net.ddns.mindustry.database.schema.Tables.ACCOUNT_ROLE;
import static net.ddns.mindustry.database.schema.Tables.ACCOUNT_ROLE_HISTORY;

@NullMarked
public record RoleQueriesImpl(DatabaseImpl database) implements RoleQueries {

    @Override
    public boolean hasRole(Account account, Role role) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(role);
        return database().dsl()
                .selectOne()
                .from(ACCOUNT_ROLE)
                .where(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())
                        .and(ACCOUNT_ROLE.ROLE_ID.eq(role.id())))
                .execute() == 1;
    }

    @Override
    public boolean hasRole(Account account, String roleName) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(roleName);
        return findRole(roleName)
                .map(foundRole -> hasRole(account, foundRole))
                .orElse(false);
    }

    @Override
    public boolean hasPermission(Account account, Permission permission) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(permission);
        return database().dsl()
                .selectOne() // One is enough, even if the account has multiple roles with the same permissions.
                .from(Tables.ROLE_PERMISSION)
                .innerJoin(ACCOUNT_ROLE)
                .on(ACCOUNT_ROLE.ROLE_ID.eq(Tables.ROLE_PERMISSION.ROLE_ID)
                        .and(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())))
                .where(Tables.ROLE_PERMISSION.PERMISSION_ID.eq(permission.id()))
                .execute() == 1;
    }

    @Override
    public boolean hasPermission(Account account, String permission) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(permission);
        return findPermission(permission)
                .map(foundPerm -> hasPermission(account, foundPerm))
                .orElse(false);
    }

    @Override
    public Optional<Role> findRole(String name) {
        Objects.requireNonNull(name);
        return database().dsl()
                .selectFrom(Tables.ROLE)
                .where(Tables.ROLE.NAME.eq(name))
                .fetchOptionalInto(Role.class);
    }

    @Override
    public Optional<Role> findRole(Integer id) {
        Objects.requireNonNull(id);
        return database().dsl()
                .selectFrom(Tables.ROLE)
                .where(Tables.ROLE.ID.eq(id))
                .fetchOptionalInto(Role.class);
    }

    @Override
    public List<Role> listRoles() {
        return database().dsl()
                .selectFrom(Tables.ROLE)
                .fetchInto(Role.class);
    }

    @Override
    public List<Role> accountRoles(Account account) {
        Objects.requireNonNull(account);
        return database().dsl()
                .select()
                .from(ACCOUNT_ROLE)
                .innerJoin(Tables.ROLE).on(Tables.ROLE.ID.eq(ACCOUNT_ROLE.ROLE_ID))
                .where(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id()))
                .fetchInto(Role.class);
    }

    @Override
    public Optional<Permission> findPermission(String permission) {
        Objects.requireNonNull(permission);
        return database().dsl()
                .selectFrom(Tables.PERMISSION)
                .where(Tables.PERMISSION.PROPERTY.eq(permission))
                .fetchOptionalInto(Permission.class);
    }

    @Override
    public boolean newPermission(String permission) {
        Objects.requireNonNull(permission);
        return database().dsl()
                .insertInto(Tables.PERMISSION)
                .set(Tables.PERMISSION.PROPERTY, permission)
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public void updatePermission(Permission permission, String newPermission) {
        Objects.requireNonNull(permission);
        Objects.requireNonNull(newPermission);
        database().dsl()
                .update(Tables.PERMISSION)
                .set(Tables.PERMISSION.PROPERTY, newPermission)
                .where(Tables.PERMISSION.ID.eq(permission.id()))
                .execute();
    }

    @Override
    public boolean deletePermission(String permission) {
        return database().dsl()
                .deleteFrom(Tables.PERMISSION)
                .where(Tables.PERMISSION.PROPERTY.eq(permission))
                .execute() == 1;
    }

    @Override
    public boolean newRole(String name, String hexColor, String symbol, short priority) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(hexColor);
        Objects.requireNonNull(symbol);
        return database().dsl()
                .insertInto(Tables.ROLE)
                .set(Tables.ROLE.NAME, name)
                .set(Tables.ROLE.COLOR, hexColor)
                .set(Tables.ROLE.SYMBOL, symbol)
                .set(Tables.ROLE.PRIORITY, priority)
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public void updateRole(Role role, String name, String hexColor, String symbol, short priority) {
        database().dsl()
                .update(Tables.ROLE)
                .set(Tables.ROLE.NAME, name)
                .set(Tables.ROLE.COLOR, hexColor)
                .set(Tables.ROLE.SYMBOL, symbol)
                .set(Tables.ROLE.PRIORITY, priority)
                .execute();
    }

    @Override
    public void deleteRole(Role role) {
        Objects.requireNonNull(role);
        database().dsl()
                .deleteFrom(Tables.ROLE)
                .where(Tables.ROLE.ID.eq(role.id()))
                .execute();
    }

    @Override
    public boolean linkPermission(Permission permission, Role role) {

        Objects.requireNonNull(role);
        Objects.requireNonNull(permission);

        return database().dsl()
                .insertInto(Tables.ROLE_PERMISSION)
                .set(Tables.ROLE_PERMISSION.ROLE_ID, role.id())
                .set(Tables.ROLE_PERMISSION.PERMISSION_ID, permission.id())
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public boolean unlinkPermission(Permission permission, Role role) {

        Objects.requireNonNull(role);
        Objects.requireNonNull(permission);

        return database().dsl()
                .deleteFrom(Tables.ROLE_PERMISSION)
                .where(Tables.ROLE_PERMISSION.ROLE_ID.eq(role.id())
                        .and(Tables.ROLE_PERMISSION.PERMISSION_ID.eq(permission.id())))
                .execute() == 1;
    }

    @Override
    public boolean grantRole(Account account, Role role) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(role);

        return database().dsl()
                .insertInto(ACCOUNT_ROLE)
                .set(ACCOUNT_ROLE.ACCOUNT_ID, account.id())
                .set(ACCOUNT_ROLE.ROLE_ID, role.id())
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public boolean revokeRole(Account account, Role role) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(role);

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final AccountRole revoked = tDsl.deleteFrom(ACCOUNT_ROLE)
                    .where(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())
                            .and(ACCOUNT_ROLE.ROLE_ID.eq(role.id())))
                    .returningResult()
                    .fetchOptionalInto(AccountRole.class)
                    .orElse(null);
            // In case the role has already been revoked or the user never had it.
            if (revoked == null) return false;

            return tDsl.insertInto(ACCOUNT_ROLE_HISTORY)
                    .set(ACCOUNT_ROLE_HISTORY.ACCOUNT_ID, revoked.accountId())
                    .set(ACCOUNT_ROLE_HISTORY.ROLE_ID, revoked.roleId())
                    .set(ACCOUNT_ROLE_HISTORY.GRANT_DATE, revoked.grantDate())
                    .execute() == 1;
        });
    }

    @Override
    public Set<AccountRoleHistory> accountRolesHistory(Account account) {
        Objects.requireNonNull(account);
        return database().dsl()
                .selectFrom(ACCOUNT_ROLE_HISTORY)
                .where(ACCOUNT_ROLE_HISTORY.ACCOUNT_ID.eq(account.id()))
                .fetchStreamInto(AccountRoleHistory.class)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<AccountRoleHistory> accountRoleHistory(Account account, Role role) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(role);
        return database().dsl()
                .selectFrom(ACCOUNT_ROLE_HISTORY)
                .where(ACCOUNT_ROLE_HISTORY.ACCOUNT_ID.eq(account.id()).and(ACCOUNT_ROLE_HISTORY.ROLE_ID.eq(role.id())))
                .fetchStreamInto(AccountRoleHistory.class)
                .collect(Collectors.toSet());
    }
}
