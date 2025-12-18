package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import arc.util.Log
import mindustry.gen.Player
import net.ddns.mindustry.database.client.PunishmentQueries.Issuer
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.schema.tables.pojos.Permission

class LookupBans(handler: CommandHandler) : PrivilegedClientCommand(handler) {
    companion object {
        private val permission: Permission
        private const val PERMISSION_NAME = "ban"

        init {
            description = "Looks up the active bans that a player has."
            parameters = "<account-name>"

            database!!.role().newPermission(PERMISSION_NAME)
            permission = database!!.role().findPermission(PERMISSION_NAME).get()
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        val account = hasPermission(permission, player) ?: return
        val targetName = arguments[0]

        val target = database!!.account().find(targetName)

        if (target.isEmpty) {
            player.sendMessage("[scarlet]Couldn't find that player!")
            return
        }

        val bans = database!!.punishment().activeBans(target.get())

        if (bans.size == 0) {
            player.sendMessage("That player has no active bans.")
            return
        }

        player.sendMessage("Active bans:")
        player.sendMessage("    id (expires: DD-MM-YYYY): \"reason\" — issuer")
        for (ban in bans) {
            val expiration = ban.expirationDate
            val possibleIssuer = database!!.punishment().findIssuer(ban.issuerId)
            val issuerName: String

            if (possibleIssuer.isEmpty) {
                Log.warn("Issuer is empty for ban @", ban.id)
                issuerName = "Unknown issuer"
            } else {
                issuerName = when (val issuer = possibleIssuer.get()) {
                    is Issuer.Console -> {
                        "Console"
                    }

                    is Issuer.Player -> {
                        issuer.account.username
                    }
                }
            }

            val toSend = String.format("    %d (expires: %d-%d-%d): \"%s\" — %s", ban.id, expiration.dayOfMonth,
                expiration.monthValue, expiration.year, ban.reason, issuerName)
            player.sendMessage(toSend)
        }
    }
}