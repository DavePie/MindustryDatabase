package net.ddns.mindustry.database.plugin.commands.client.unprivileged

import mindustry.gen.Player
import net.ddns.mindustry.segment.TextInputHandler

var playerToUsernameMap: MutableMap<Player, String> = mutableMapOf()
var playerToDisplayName: MutableMap<Player, String> = mutableMapOf()

var textInputHandler: TextInputHandler = TextInputHandler()
