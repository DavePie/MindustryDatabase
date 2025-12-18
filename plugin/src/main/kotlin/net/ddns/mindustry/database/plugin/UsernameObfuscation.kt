package net.ddns.mindustry.database.plugin

import arc.util.Log
import mindustry.gen.Player

private val usernameMap: MutableMap<String, String> = mutableMapOf()

fun obfuscateUsername(player: Player) {
    usernameMap[player.uuid()] = player.coloredName()
    player.name("")
}

fun deobfuscateUsername(player: Player) {
    if (!usernameMap.contains(player.uuid())) {
        Log.warn("Couldn't deobfuscate username for @. UUID is not registered.", player.uuid())
        return
    }

    player.name(usernameMap[player.uuid()])
    usernameMap.remove(player.uuid())
}

fun removeUsername(player: Player) {
    usernameMap.remove(player.uuid())
}
