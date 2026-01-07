package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import mindustry.gen.Groups
import mindustry.gen.Player
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.schema.tables.pojos.Account
import net.ddns.mindustry.database.schema.tables.pojos.Permission
import kotlin.jvm.optionals.getOrNull

class OnlinePlayers(handler: CommandHandler) : PrivilegedClientCommand(handler) {
    companion object {
        private val permission: Permission
        private const val PERMISSION_NAME = "online-players"

        init {
            description = "Looks up the currently online players."
            parameters = ""

            database!!.role().newPermission(PERMISSION_NAME)
            permission = database!!.role().findPermission(PERMISSION_NAME).get()
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        hasPermission(permission, player) ?: return

        val targets = mutableMapOf<String, Account?>()
        for (user in Groups.player) {
            val target = database!!.account().find(user.ip(), user.uuid())

            // exclude banned players
            if (target.isPresent && database!!.punishment().activeBans(target.get()).size != 0) continue;

            targets[user.coloredName()] = target.getOrNull()
        }

        var returnMessage = "[gold]Online players[gray]:[white]"
        for (target in targets) {
            if (target.value == null) continue

            returnMessage += String.format("\n    [white]%s[gray]: [white]%s", target.key, target.value!!.username)
        }

        player.sendMessage(returnMessage)
    }
}