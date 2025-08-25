package net.ddns.mindustry.database.plugin

import mindustry.Vars.netServer
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.net.Administration.ChatFilter
import net.ddns.mindustry.database.plugin.Main.Companion.database

fun loadChatFilters() {
    netServer.admins.chatFilters.add(ChatFilter(::noDerelict))
    netServer.admins.chatFilters.add(ChatFilter(::noBanned))
}

// Remember... no derelict.
private fun noDerelict(player: Player, message: String): String? {
    return if ((player.team().id == Team.derelict.id) && (!message.startsWith("/"))) null else message
}

private fun noBanned(player: Player, message: String): String? {
    val account = database!!.account().find(player.ip(), player.uuid())

    if (account.isEmpty) {
        return null
    } else if (database!!.punishment().activeBans(account.get()).size != 0) {
        player.sendMessage("[scarlet]You are banned!")
        return null
    }

    return message
}
