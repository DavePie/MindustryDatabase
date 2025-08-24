package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import mindustry.gen.Player
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.commands.client.BaseClientCommand
import net.ddns.mindustry.database.schema.tables.pojos.Account
import net.ddns.mindustry.database.schema.tables.pojos.Permission

sealed class PrivilegedClientCommand(handler: CommandHandler) : BaseClientCommand(handler) {
    fun hasPermission(permission: Permission, player: Player): Account? {
        val account = database!!.account().find(player.ip(), player.uuid())

        if (account.isEmpty) {
            player.sendMessage("[scarlet]Your account could not be found.")
            return null
        }

        if (!database!!.role().hasPermissions(account.get(), permission)) {
            player.sendMessage("[scarlet]You do not have permission to run this command.")
            return null
        }

        return account.get()
    }
}