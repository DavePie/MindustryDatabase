package net.ddns.mindustry.database.plugin

import mindustry.Vars.netServer
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.net.Administration.ChatFilter

fun loadChatFilters() {
    netServer.admins.chatFilters.add(ChatFilter(::noDerelict))
}

// Remember... no derelict.
private fun noDerelict(player: Player, message: String): String? {
    return if ((player.team().id == Team.derelict.id) && (!message.startsWith("/"))) null else message
}
