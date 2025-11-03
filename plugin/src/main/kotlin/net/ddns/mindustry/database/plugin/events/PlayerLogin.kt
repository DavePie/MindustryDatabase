package net.ddns.mindustry.database.plugin.events

import mindustry.gen.Player
import net.ddns.mindustry.database.schema.tables.pojos.Account

// Kotlin so good that the lack of boilerplate makes a class look suspiciously incomplete.
class PlayerLogin(val player: Player, val account: Account)