package net.ddns.mindustry.database.plugin.commands.server.roles

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database

class DeleteRole(handler: CommandHandler) : BaseRoleCommand(handler) {
    companion object {
        init {
            description = "Deletes a role from the database."
            parameters = "<id>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val id = arguments[0]
        val role = database!!.role().findRole(id.toInt())

        if (role.isEmpty) {
            Log.err("Couldn't find a role by that ID.")
            return;
        }

        database!!.role().deleteRole(role.get())
    }
}