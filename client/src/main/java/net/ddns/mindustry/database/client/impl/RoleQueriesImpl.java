package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.RoleQueries;
import net.ddns.mindustry.database.schema.Tables;
import net.ddns.mindustry.database.schema.tables.pojos.*;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import java.util.*;
import java.util.stream.Collectors;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record RoleQueriesImpl(DatabaseImpl database) implements RoleQueries {

    @Override
    public boolean hasRoles(Account account, Role... roles) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(roles);
        if (roles.length == 0) throw new IllegalArgumentException("At least one role must be provided.");

        final Set<Integer> ints = Arrays.stream(roles)
                .map(Role::id)
                .collect(Collectors.toSet());

        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectCount()
                .from(ACCOUNT_ROLE)
                .where(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())
                        .and(ACCOUNT_ROLE.ROLE_ID.in(ints)))
                .fetchSingleInto(Integer.class) == ints.size());
    }

    @Override
    public boolean hasPermissions(Account account, Permission... permissions) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(permissions);
        if (permissions.length == 0) throw new IllegalArgumentException("At least one permission must be provided.");

        final Set<Integer> ints = Arrays.stream(permissions)
                .map(Permission::id)
                .collect(Collectors.toSet());

        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .select(DSL.countDistinct(ROLE_PERMISSION.PERMISSION_ID))
                .from(ROLE_PERMISSION)
                .innerJoin(ACCOUNT_ROLE)
                .on(ACCOUNT_ROLE.ROLE_ID.eq(ROLE_PERMISSION.ROLE_ID).and(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id())))
                .where(ROLE_PERMISSION.PERMISSION_ID.in(ints))
                .fetchSingleInto(Integer.class) == ints.size());
    }

    @Override
    public Optional<Role> findRole(String name) {
        Objects.requireNonNull(name);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(Tables.ROLE)
                .where(Tables.ROLE.NAME.eq(name))
                .fetchOptionalInto(Role.class));
    }

    @Override
    public Optional<Role> findRole(Integer id) {
        Objects.requireNonNull(id);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(Tables.ROLE)
                .where(Tables.ROLE.ID.eq(id))
                .fetchOptionalInto(Role.class));
    }

    @Override
    public List<Role> listRoles() {
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(Tables.ROLE)
                .fetchInto(Role.class));
    }

    @Override
    public List<Role> accountRoles(Account account) {
        Objects.requireNonNull(account);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .select()
                .from(ACCOUNT_ROLE)
                .innerJoin(ROLE).on(ROLE.ID.eq(ACCOUNT_ROLE.ROLE_ID))
                .where(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id()))
                .fetchInto(ROLE) // I specify which table I want for the mapping.
                .into(Role.class));
    }

    @Override
    public Optional<Permission> findPermission(String permission) {
        Objects.requireNonNull(permission);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(Tables.PERMISSION)
                .where(Tables.PERMISSION.PROPERTY.eq(permission))
                .fetchOptionalInto(Permission.class));
    }

    @Override
    public boolean newPermission(String permission) {
        Objects.requireNonNull(permission);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .insertInto(Tables.PERMISSION)
                .set(Tables.PERMISSION.PROPERTY, permission)
                .onConflictDoNothing()
                .execute() == 1);
    }

    @Override
    public void updatePermission(Permission permission, String newPermission) {
        Objects.requireNonNull(permission);
        Objects.requireNonNull(newPermission);
        database().dsl().transactionResult(ctx -> ctx.dsl()
                .update(Tables.PERMISSION)
                .set(Tables.PERMISSION.PROPERTY, newPermission)
                .where(Tables.PERMISSION.ID.eq(permission.id()))
                .execute());
    }

    @Override
    public boolean deletePermission(Permission permission) {
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .deleteFrom(Tables.PERMISSION)
                .where(Tables.PERMISSION.ID.eq(permission.id()))
                .execute() == 1);
    }

    @Override
    public Optional<Role> newRole(String name, String hexColor, String symbol, short priority) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(hexColor);
        Objects.requireNonNull(symbol);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .insertInto(Tables.ROLE)
                .set(Tables.ROLE.NAME, name)
                .set(Tables.ROLE.COLOR, hexColor)
                .set(Tables.ROLE.SYMBOL, symbol)
                .set(Tables.ROLE.PRIORITY, priority)
                .onConflictDoNothing()
                .returningResult()
                .fetchOptionalInto(Role.class));
    }

    @Override
    public void updateRole(Role role, String name, String hexColor, String symbol, short priority) {
        database().dsl().transactionResult(ctx -> ctx.dsl()
                .update(Tables.ROLE)
                .set(Tables.ROLE.NAME, name)
                .set(Tables.ROLE.COLOR, hexColor)
                .set(Tables.ROLE.SYMBOL, symbol)
                .set(Tables.ROLE.PRIORITY, priority)
                .execute());
    }

    @Override
    public void deleteRole(Role role) {
        Objects.requireNonNull(role);
        database().dsl().transactionResult(ctx -> ctx.dsl()
                .deleteFrom(Tables.ROLE)
                .where(Tables.ROLE.ID.eq(role.id()))
                .execute());
    }

    @Override
    public int linkPermissions(Role role, Permission... permissions) {

        Objects.requireNonNull(role);
        Objects.requireNonNull(permissions);
        if (permissions.length == 0) throw new IllegalArgumentException("At least one permission must be provided.");

        return database().dsl().transactionResult(ctx -> {
            var query = ctx.dsl().insertInto(ROLE_PERMISSION, ROLE_PERMISSION.ROLE_ID, ROLE_PERMISSION.PERMISSION_ID);
            for (final Permission perm : permissions) {
                query = query.values(role.id(), perm.id());
            }
            return query.onConflictDoNothing().execute();
        });
    }

    @Override
    public int unlinkPermissions(Role role, Permission... permissions) {

        Objects.requireNonNull(role);
        Objects.requireNonNull(permissions);
        if (permissions.length == 0) throw new IllegalArgumentException("At least one permission must be provided.");

        final Set<Integer> ints = Arrays.stream(permissions)
                .map(Permission::id)
                .collect(Collectors.toSet());

        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .deleteFrom(ROLE_PERMISSION)
                .where(ROLE_PERMISSION.ROLE_ID.eq(role.id()).and(ROLE_PERMISSION.PERMISSION_ID.in(ints)))
                .execute());
    }

    @Override
    public List<Permission> listPermissions() {
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(PERMISSION)
                .fetchInto(Permission.class));
    }

    @Override
    public List<Permission> rolePermissions(Role role) {
        Objects.requireNonNull(role);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .select()
                .from(ROLE_PERMISSION)
                .innerJoin(PERMISSION).on(PERMISSION.ID.eq(ROLE_PERMISSION.ROLE_ID))
                .where(ROLE.ID.eq(role.id()))
                .fetchInto(Permission.class));
    }

    @Override
    public int grantRoles(Account account, Role... roles) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(roles);
        if (roles.length == 0) throw new IllegalArgumentException("At least one role must be provided.");

        return database().dsl().transactionResult(ctx -> {
            var query = ctx.dsl().insertInto(ACCOUNT_ROLE, ACCOUNT_ROLE.ACCOUNT_ID, ACCOUNT_ROLE.ROLE_ID);
            for (var role : roles) query = query.values(account.id(), role.id());
            return query.onConflictDoNothing().execute();
        });
    }

    @Override
    public int revokeRoles(Account account, Role... roles) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(roles);
        if (roles.length == 0) throw new IllegalArgumentException("At least one role must be provided.");

        final Set<Integer> ints = Arrays.stream(roles)
                .map(Role::id)
                .collect(Collectors.toSet());

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final List<AccountRole> rolesRevoked = tDsl.deleteFrom(ACCOUNT_ROLE)
                    .where(ACCOUNT_ROLE.ACCOUNT_ID.eq(account.id()).and(ACCOUNT_ROLE.ROLE_ID.in(ints)))
                    .returningResult()
                    .fetchInto(AccountRole.class);

            // In case the roles have already been revoked or the user never had them.
            if (rolesRevoked.isEmpty()) return 0;

            var query = tDsl.insertInto(ACCOUNT_ROLE_HISTORY,
                    ACCOUNT_ROLE_HISTORY.ACCOUNT_ID,
                    ACCOUNT_ROLE_HISTORY.ROLE_ID,
                    ACCOUNT_ROLE_HISTORY.GRANT_DATE);
            for (var revoked : rolesRevoked) query = query.values(account.id(), revoked.roleId(), revoked.grantDate());
            return query.execute();
        });
    }

    @Override
    public Set<AccountRoleHistory> accountRolesHistory(Account account) {
        Objects.requireNonNull(account);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(ACCOUNT_ROLE_HISTORY)
                .where(ACCOUNT_ROLE_HISTORY.ACCOUNT_ID.eq(account.id()))
                .fetchStreamInto(AccountRoleHistory.class)
                .collect(Collectors.toSet()));
    }

    @Override
    public Set<AccountRoleHistory> accountRoleHistory(Account account, Role role) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(role);
        return database().dsl().transactionResult(ctx -> ctx.dsl()
                .selectFrom(ACCOUNT_ROLE_HISTORY)
                .where(ACCOUNT_ROLE_HISTORY.ACCOUNT_ID.eq(account.id()).and(ACCOUNT_ROLE_HISTORY.ROLE_ID.eq(role.id())))
                .fetchStreamInto(AccountRoleHistory.class)
                .collect(Collectors.toSet()));
    }
}
