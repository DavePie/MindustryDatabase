package net.ddns.mindustry.database.plugin

import mindustry.Vars.netServer
import mindustry.net.Administration
import net.ddns.mindustry.database.plugin.Main.Companion.database

fun loadActionFilters() {
    netServer.admins.actionFilters.add(Administration.ActionFilter(::noBanned))
}

// literally 1984...
private fun noBanned(action: Administration.PlayerAction): Boolean {
    val account = database!!.account().find(action.player.ip(), action.player.uuid())

    if (account.isEmpty) {
        return false
    } else if (database!!.punishment().activeBans(account.get()).size != 0) {
        action.player.sendMessage("[scarlet]You are banned!")
        return false
    }

    return true
}
