package net.ddns.mindustry.database.plugin.commands.server.permissions

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database

class RevokePermission(handler: CommandHandler) : BasePermissionCommand(handler) {
    companion object {
        init {
            description = "Revokes a permission from a role."
            parameters = "<role-name> <permissions...>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val roleName = arguments[0]
        val role = database!!.role().findRole(roleName)

        if (role.isEmpty) {
            Log.err("Couldn't find a role by that name.")
            return
        }

        for (i in 1..<arguments.size) {
            val permissionName = arguments[i]
            val permission = database!!.role().findPermission(permissionName)

            if (permission.isEmpty) {
                Log.warn("Couldn't find permission @ — skipping permission.", permissionName)
                continue
            }

            database!!.role().unlinkPermissions(role.get(), permission.get())
            Log.info("Revoked permission: @.", permissionName)
        }
    }
}