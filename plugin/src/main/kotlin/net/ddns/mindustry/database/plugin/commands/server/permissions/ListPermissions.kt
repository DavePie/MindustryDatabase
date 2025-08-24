package net.ddns.mindustry.database.plugin.commands.server.permissions

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.schema.tables.pojos.Permission

class ListPermissions(handler: CommandHandler) : BasePermissionCommand(handler) {
    companion object {
        init {
            description = "List all permissions in the database."
            parameters = "[role]"
        }
    }

    override fun runner(arguments: Array<String>) {
        if (arguments.size == 1) {
            val roleName = arguments[0]
            listRolePermissions(roleName)
            return
        }

        listPermissions()
    }

    private fun listRolePermissions(roleName: String) {
        val role = database!!.role().findRole(roleName)

        if (role.isEmpty) {
            Log.err("Couldn't find role @.", roleName)
            return;
        }

        val permissions = database!!.role().rolePermissions(role.get())
        logPermissions(permissions)
    }

    private fun listPermissions() {
        val permissions = database!!.role().listPermissions()
        logPermissions(permissions)
    }

    private fun logPermissions(permissions: List<Permission>) {
        for (permission in permissions) {
            Log.info("- @ (@): (@) @", permission.id, permission.property)
        }
    }
}