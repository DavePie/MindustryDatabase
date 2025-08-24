package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import mindustry.gen.Call
import mindustry.gen.Player
import mindustry.net.Administration
import net.ddns.mindustry.database.client.PunishmentQueries.Issuer
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs
import net.ddns.mindustry.database.schema.tables.pojos.Permission
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class Ban(handler: CommandHandler) : PrivilegedClientCommand(handler) {
    companion object {
        val banPermission: Permission

        init {
            description = "Bans an account via its username. Display names are usernames are separate.."
            parameters = "<account-name> <duration> <reason...>"

            database!!.role().newPermission("ban")
            banPermission = database!!.role().findPermission("ban").get()
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        val issuerAccount = hasPermission(banPermission, player) ?: return
        val issuer = Issuer.Player(issuerAccount)

        val targetName = arguments[0]
        val durationString = arguments[1]
        val reason = arguments[2]

        val target = database!!.account().find(targetName)
        val duration = Duration.parse(durationString)
        val server = database!!.server().find(PluginConfigs.configServerIP.string(), Administration.Config.port.num())

        database!!.punishment().ban(target.get(), issuer, reason, server.get(), duration.toJavaDuration())
        player.sendMessage("$targetName was banned.")
    }
}