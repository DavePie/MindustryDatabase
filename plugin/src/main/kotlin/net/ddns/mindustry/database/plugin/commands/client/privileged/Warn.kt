package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import mindustry.gen.Player
import mindustry.net.Administration
import net.ddns.mindustry.database.client.PunishmentQueries.Issuer
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs
import net.ddns.mindustry.database.schema.tables.pojos.Permission

class Warn(handler: CommandHandler) : PrivilegedClientCommand(handler) {
    companion object {
        val warnPermission: Permission

        init {
            description = "Warns a player."
            parameters = "<account-name> <reason...>"

            database!!.role().newPermission("ban")
            warnPermission = database!!.role().findPermission("ban").get()
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        val issuerAccount = hasPermission(warnPermission, player) ?: return
        val issuer = Issuer.Player(issuerAccount)

        val targetName = arguments[0]
        val reason = arguments[1]

        val target = database!!.account().find(targetName)
        val server = database!!.server().find(PluginConfigs.configServerIP.string(), Administration.Config.port.num())

        if (target.isEmpty) {
            player.sendMessage("[scarlet]Couldn't find that player!")
            return
        }

        database!!.punishment().warn(target.get(), issuer, reason, server.get())
        player.sendMessage("$targetName was banned.")
    }
}