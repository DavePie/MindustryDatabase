package net.ddns.mindustry.database.plugin.commands.server.roles

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.schema.tables.pojos.Role

class ListRoles(handler: CommandHandler) : RoleCommand(handler) {
    companion object {
        init {
            description = "Lists all roles that are in the database."
            parameters = "[account-username]"
        }
    }

    override fun runner(arguments: Array<String>) {
        if (arguments.size == 1) {
            listPlayerRoles(arguments[0])
            return
        }

        listAllRoles()
    }

    private fun listPlayerRoles(accountUsername: String) {
        val account = database!!.auth().find(accountUsername)

        if (account.isEmpty) {
            Log.err("Couldn't find an account by that name.")
        }

        val accountRoles = database!!.role().listRoles(account.get().id)
        logRoles(accountRoles)
    }

    private fun listAllRoles() {
        val roles = database!!.role().listRoles()
        logRoles(roles)
    }

    /**
     * Prints all the roles in a roles list. This is done via Log.info().
     * @param roles A List containing Role objects. The roles in this list will be printed via Log.info().
     */
    private fun logRoles(roles: List<Role>) {
        for (role in roles) {
            Log.info(String.format("- %d (%d): (%s) %s", role.id, role.priority, role.symbol, role.name))
        }
    }
}