package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.RoleQueries;
import net.ddns.mindustry.database.schema.Tables;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Permission;
import net.ddns.mindustry.database.schema.tables.pojos.Role;
import org.jooq.DSLContext;
import java.util.Objects;
import java.util.Optional;

public record RoleQueriesImpl(DSLContext dsl) implements RoleQueries {

    public RoleQueriesImpl {
        Objects.requireNonNull(dsl);
    }

    @Override
    public boolean hasRole(Account account, Role role) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(role);
        return dsl().selectOne()
                .from(Tables.ACCOUNT_ROLE)
                .where(Tables.ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())
                        .and(Tables.ACCOUNT_ROLE.ROLE_ID.eq(role.id())))
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
        return dsl().selectOne() // One is enough, even if the account has multiple roles with the same permissions.
                .from(Tables.ROLE_PERMISSION)
                .innerJoin(Tables.ACCOUNT_ROLE)
                .on(Tables.ACCOUNT_ROLE.ROLE_ID.eq(Tables.ROLE_PERMISSION.ROLE_ID)
                        .and(Tables.ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())))
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
        return dsl().selectFrom(Tables.ROLE)
                .where(Tables.ROLE.NAME.eq(name))
                .fetchOptionalInto(Role.class);
    }

    @Override
    public Optional<Permission> findPermission(String permission) {
        Objects.requireNonNull(permission);
        return dsl().selectFrom(Tables.PERMISSION)
                .where(Tables.PERMISSION.PROPERTY.eq(permission))
                .fetchOptionalInto(Permission.class);
    }

    @Override
    public boolean newPermission(String permission) {
        Objects.requireNonNull(permission);
        return dsl().insertInto(Tables.PERMISSION)
                .set(Tables.PERMISSION.PROPERTY, permission)
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public void updatePermission(Permission permission, String newPermission) {
        Objects.requireNonNull(permission);
        Objects.requireNonNull(newPermission);
        dsl().update(Tables.PERMISSION)
                .set(Tables.PERMISSION.PROPERTY, newPermission)
                .where(Tables.PERMISSION.ID.eq(permission.id()))
                .execute();
    }

    @Override
    public boolean deletePermission(String permission) {
        return dsl().deleteFrom(Tables.PERMISSION)
                .where(Tables.PERMISSION.PROPERTY.eq(permission))
                .execute() == 1;
    }

    @Override
    public boolean newRole(String name, String hexColor, String symbol, short priority) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(hexColor);
        Objects.requireNonNull(symbol);
        return dsl().insertInto(Tables.ROLE)
                .set(Tables.ROLE.NAME, name)
                .set(Tables.ROLE.COLOR, hexColor)
                .set(Tables.ROLE.SYMBOL, symbol)
                .set(Tables.ROLE.PRIORITY, priority)
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public void updateRole(Role role, String name, String hexColor, String symbol, short priority) {
        dsl().update(Tables.ROLE)
                .set(Tables.ROLE.NAME, name)
                .set(Tables.ROLE.COLOR, hexColor)
                .set(Tables.ROLE.SYMBOL, symbol)
                .set(Tables.ROLE.PRIORITY, priority)
                .execute();
    }

    @Override
    public void deleteRole(Role role) {
        Objects.requireNonNull(role);
        dsl().deleteFrom(Tables.ROLE)
                .where(Tables.ROLE.ID.eq(role.id()))
                .execute();
    }

    @Override
    public boolean linkPermission(Permission permission, Role role) {

        Objects.requireNonNull(role);
        Objects.requireNonNull(permission);

        return dsl().insertInto(Tables.ROLE_PERMISSION)
                .set(Tables.ROLE_PERMISSION.ROLE_ID, role.id())
                .set(Tables.ROLE_PERMISSION.PERMISSION_ID, permission.id())
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public boolean unlinkPermission(Permission permission, Role role) {

        Objects.requireNonNull(role);
        Objects.requireNonNull(permission);

        return dsl().deleteFrom(Tables.ROLE_PERMISSION)
                .where(Tables.ROLE_PERMISSION.ROLE_ID.eq(role.id())
                        .and(Tables.ROLE_PERMISSION.PERMISSION_ID.eq(permission.id())))
                .execute() == 1;
    }

    @Override
    public boolean grantRole(Account account, Role role) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(role);

        return dsl().insertInto(Tables.ACCOUNT_ROLE)
                .set(Tables.ACCOUNT_ROLE.ACCOUNT_ID, account.id())
                .set(Tables.ACCOUNT_ROLE.ROLE_ID, role.id())
                .onConflictDoNothing()
                .execute() == 1;
    }

    @Override
    public boolean revokeRole(Account account, Role role) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(role);

        return dsl().deleteFrom(Tables.ACCOUNT_ROLE)
                .where(Tables.ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())
                        .and(Tables.ACCOUNT_ROLE.ROLE_ID.eq(role.id())))
                .execute() == 1;
    }
}
