package net.ddns.mindustry.database.plugin.commands.server.roles

import arc.util.CommandHandler
import arc.util.Log
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.applyRoleTag
import net.ddns.mindustry.database.plugin.findOnlinePlayer

class GrantRole(handler: CommandHandler) : BaseRoleCommand(handler) {
    companion object {
        init {
            description = "Grants a role to an account."
            parameters = "<account-username> <role-name>"
        }
    }

    override fun runner(arguments: Array<String>) {
        val accountUsername = arguments[0]
        val roleName = arguments[1]

        val account = database!!.account().find(accountUsername)
        val role = database!!.role().findRole(roleName)

        if (account.isEmpty) {
            Log.err("Couldn't find an account by that username.")
            return
        } else if (role.isEmpty) {
            Log.err("Couldn't find a role by that name.")
            return
        }

        database!!.role().grantRoles(account.get(), role.get())

        findOnlinePlayer(account.get().username)?.let { applyRoleTag(it, account.get()) }
        Log.info("Role granted.")
    }
}