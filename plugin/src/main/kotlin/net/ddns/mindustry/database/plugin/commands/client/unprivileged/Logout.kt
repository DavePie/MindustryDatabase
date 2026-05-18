package net.ddns.mindustry.database.plugin.commands.client.unprivileged

import arc.util.CommandHandler
import mindustry.game.Team
import mindustry.gen.Player
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.obfuscateUsername

class Logout(handler: CommandHandler) : UnprivilegedClientCommand(handler) {
    companion object {
        init {
            description = "Logs you out of the account you're signed in to."
            parameters = ""
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        val account = database!!.account().find(player.ip(), player.uuid())

        if (account.isEmpty) {
            player.sendMessage("[orange]There is either no active session or you're not logged in.")
            return
        }

        database!!.account().logout(account.get())
        player.team(Team.derelict)
        player.unit().kill()
        obfuscateUsername(player)
        player.sendMessage("Logged out successfully.")
    }
}