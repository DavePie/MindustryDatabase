package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import mindustry.gen.Player
import net.ddns.mindustry.database.client.PunishmentQueries
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.schema.tables.pojos.Permission

class RemoveBan(handler: CommandHandler) : PrivilegedClientCommand(handler) {
    companion object {
        private val permission: Permission
        private const val PERMISSION_NAME = "ban"

        init {
            description = "Removes a ban from a player."
            parameters = "<ban-id>"

            database!!.role().newPermission(PERMISSION_NAME)
            permission = database!!.role().findPermission(PERMISSION_NAME).get()
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        val account = hasPermission(permission, player) ?: return
        val issuer = PunishmentQueries.Issuer.Player(account)

        val ban = database!!.punishment().findBan(arguments[0].toInt())
        if (ban.isEmpty) {
            player.sendMessage("[scarlet]Couldn't find that ban!")
            return
        }

        database!!.punishment().unban(ban.get(), issuer)
        player.sendMessage("Removed ban " + ban.get().id)
    }
}