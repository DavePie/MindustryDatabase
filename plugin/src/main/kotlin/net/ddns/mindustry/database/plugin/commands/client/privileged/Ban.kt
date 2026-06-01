package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import mindustry.gen.Player
import net.ddns.mindustry.database.client.PunishmentQueries.Issuer
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.commands.client.privileged.ui.PunishFlow
import net.ddns.mindustry.database.plugin.currentServer
import net.ddns.mindustry.database.plugin.resolveTargetAccount
import net.ddns.mindustry.database.schema.tables.pojos.Permission
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class Ban(handler: CommandHandler) : PrivilegedClientCommand(handler) {
    companion object {
        val banPermission: Permission

        init {
            description = "Bans a player by account name, or run with no arguments to pick an online player from a menu."
            parameters = "[account-name] [duration] [reason...]"

            database!!.role().newPermission("ban")
            banPermission = database!!.role().findPermission("ban").get()
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        val issuerAccount = hasPermission(banPermission, player) ?: return

        if (arguments.isEmpty()) {
            PunishFlow.start(player, "ban")
            return
        }
        if (arguments.size < 3) {
            player.sendMessage("[scarlet]Usage: /ban <account-name> <duration> <reason...>  (or /ban with no arguments to pick from a menu)")
            return
        }

        val issuer = Issuer.Player(issuerAccount)
        val durationString = arguments[1]
        val reason = arguments[2]

        val target = resolveTargetAccount(arguments[0], player) ?: return
        val duration = Duration.parse(durationString)

        database!!.punishment().ban(target, issuer, reason, currentServer(), duration.toJavaDuration())
        player.sendMessage("${target.username} was banned.")
    }
}