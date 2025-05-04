package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Permission;
import net.ddns.mindustry.database.schema.tables.pojos.Role;
import org.jspecify.annotations.NullMarked;
import java.util.Optional;

@NullMarked
public interface RoleQueries {

    /// Checks if this account has the role.
    /// @param account the account to check.
    /// @param role the role to check against.
    /// @return true if this account has the provided role, false otherwise.
    boolean hasRole(Account account, Role role);

    /// Check if this account has the role.
    /// @param account the account to check.
    /// @param roleName the name of the role to check against.
    /// @return true if this account has the provided role,
    ///  false if the role was not found, or the user does not have it.
    boolean hasRole(Account account, String roleName);

    /// Check if this account has the permission.\
    /// It doesn't matter which role has the permission; as long as the account has it, true is returned.
    /// @param account the account to check.
    /// @param permission the permission to check against.
    /// @return true if this account has the provided permission, false otherwise.
    boolean hasPermission(Account account, Permission permission);

    /// Check if this account has the permission.\
    /// It doesn't matter which role has the permission; as long as the account has it, true is returned.
    /// @param account the account to check.
    /// @param permission the permission property to check against.
    /// @return true if this account has the provided permission, false if the permission was not found, or the user does not have it.
    boolean hasPermission(Account account, String permission);

    /// Searches the role via its name.
    /// @param name the name of the role to search.
    /// @return the role found having the name provided, if not found, an empty optional is returned.
    Optional<Role> findRole(String name);

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
    boolean deletePermission(String permission);

    /// Creates a new role given the provided information.
    /// @param name the name of the role.
    /// @param hexColor the color in hex format (RGBA) `FF0099EE`.
    /// @param symbol the symbol of the role, preferably a single character.
    /// @param priority the priority of the role, the smallest is displayed on top.
    /// @return true if created, false if already present.
    boolean newRole(String name, String hexColor, String symbol, short priority);

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
    /// @param permission the permission to link.
    /// @param role the role that will receive the permission.
    /// @return true if the link has been created, false if already linked.
    boolean linkPermission(Permission permission, Role role);

    /// Removes the wanted permission from the role.
    /// @param permission the permission to unlink.
    /// @param role the role to remove the permission from.
    /// @return true if the link has been deleted, false if the role did not have the permission.
    boolean unlinkPermission(Permission permission, Role role);

    /// Grants to the provided account the role.
    /// @param account the account to grant the role to.
    /// @param role the role to grant.
    /// @return true if granted, false if already granted.
    boolean grantRole(Account account, Role role);

    /// Revokes the role from the provided account.
    /// @param account the account to revoke the role from.
    /// @param role the role to revoke.
    /// @return true if revoked, false if the account did not have the role.
    boolean revokeRole(Account account, Role role);
}
