package net.ddns.mindustry.database.plugin.commands.server.roles

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database

class AddRole(handler: CommandHandler) : BaseRoleCommand(handler) {
    companion object {
        init {
            description = "Adds a role to the database."
            parameters = "<name> <color> <symbol> <priority>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val roleName = arguments[0]
        val roleColor = arguments[1].removePrefix("0x")
        val roleSymbol = arguments[2]
        val priority = arguments[3].toShort()

        val result = database!!.role().newRole(roleName, roleColor, roleSymbol, priority)

        if (result.isEmpty) {
            Log.err("Failed to create the new role.")
            return;
        }

        Log.info("Created the role successfully.")
    }
}