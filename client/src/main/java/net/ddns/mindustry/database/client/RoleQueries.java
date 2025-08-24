package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.AccountRoleHistory;
import net.ddns.mindustry.database.schema.tables.pojos.Permission;
import net.ddns.mindustry.database.schema.tables.pojos.Role;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleQueries {

    /// Checks if this account has the role.
    /// @param account the account to check.
    /// @param roles the roles to check against.
    /// @return true if this account has the provided roles, false otherwise.
    boolean hasRoles(Account account, Role... roles);

    /// Check if this account has the permission.\
    /// It doesn't matter which role has the permission; as long as the account has it, true is returned.
    /// @param account the account to check.
    /// @param permissions the permissions to check against.
    /// @return true if this account has the provided permissions, false otherwise.
    boolean hasPermissions(Account account, Permission... permissions);

    /// Searches the role via its name.
    /// @param name the name of the role to search.
    /// @return the role found having the name provided, if not found, an empty optional is returned.
    Optional<Role> findRole(String name);

    Optional<Role> findRole(Integer id);

    /// Lists all the roles in the database.
    /// @return a List representing all the roles in the database.
    List<Role> listRoles();

    /// @return a list of all the roles the given account has.
    List<Role> accountRoles(Account account);

    /// Searches the permission via its property.
    /// @param permission the property of the permission to search.
    /// @return the permission found having the property provided, if not found, an empty optional is returned.
    Optional<Permission> findPermission(String permission);

    /// Creates a new permission with the given property.
    /// @param permission the property of the permission to create.
    /// @return true if the permission has been added, false if already present.
    boolean newPermission(String permission);

    /// Updates the property of the permission.
    /// @param permission the permission to update.
    /// @param newPermission the new property of the permission.
    void updatePermission(Permission permission, String newPermission);

    /// Deletes the permission by its property, all links associated with the permission are also deleted.
    /// @param permission the property of the permission to delete.
    /// @return true if the permission has been deleted, false if not found.
    boolean deletePermission(Permission permission);

    /// Creates a new role given the provided information.
    /// @param name the name of the role.
    /// @param hexColor the color in hex format (RGBA) `FF0099EE`.
    /// @param symbol the symbol of the role, preferably a single character.
    /// @param priority the priority of the role, the smallest is displayed on top.
    /// @return the role if created, empty if already present.
    Optional<Role> newRole(String name, String hexColor, String symbol, short priority);

    /// Updates the role with the given information.
    /// @param role the role to modify.
    /// @param name the name of the role.
    /// @param hexColor the color in hex format (RGBA) `FF0099EE`.
    /// @param symbol the symbol of the role, preferably a single character.
    /// @param priority the priority of the role, the smallest is displayed on top.
    void updateRole(Role role, String name, String hexColor, String symbol, short priority);

    /// Deletes the role and all links associated with it.
    /// @param role the role to delete.
    void deleteRole(Role role);

    /// Assigns the wanted permission to the role.
    /// @param role the role that will receive the permission.
    /// @param permissions the permission to link.
    /// @return the number of permissions linked, if less than the permission length, some permission were already linked.
    int linkPermissions(Role role, Permission... permissions);

    /// Removes the wanted permission from the role.
    /// @param role the role to remove the permission from.
    /// @param permissions the permission to unlink.
    /// @return the number of permissions linked, if less than the permission length, some permission were already not linked.
    int unlinkPermissions(Role role, Permission... permissions);

    /// Returns a list of all the permissions in the database.
    List<Permission> listPermissions();

    /// Returns a list of the permissions that a given role has.
    /// @param role The role whose permissions should be checked.
    List<Permission> rolePermissions(Role role);

    /// Grants to the provided account the role.
    /// @param account the account to grant the role to.
    /// @param roles the role to grant.
    /// @return the number of roles granted, if less than the roles length, some roles were already granted.
    int grantRoles(Account account, Role... roles);

    /// Revokes the role from the provided account.
    /// @param account the account to revoke the role from.
    /// @param roles the role to revoke.
    /// @return the number of roles revoked, if less than the roles length, some roles were already not granted.
    int revokeRoles(Account account, Role... roles);

    Set<AccountRoleHistory> accountRolesHistory(Account account);

    Set<AccountRoleHistory> accountRoleHistory(Account account, Role role);
}
