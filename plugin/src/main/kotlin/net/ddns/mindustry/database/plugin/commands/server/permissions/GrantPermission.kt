package net.ddns.mindustry.database.plugin.commands.server.permissions

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database

class GrantPermission(handler: CommandHandler) : BasePermissionCommand(handler) {
    companion object {
        init {
            description = "Grants a permission to a role."
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

            database!!.role().linkPermissions(role.get(), permission.get())
            Log.info("Granted permission: @.", permissionName)
        }
    }
}