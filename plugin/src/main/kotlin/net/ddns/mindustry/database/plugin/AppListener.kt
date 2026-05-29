package net.ddns.mindustry.database.plugin

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.util.Log
import net.ddns.mindustry.database.plugin.events.ServerExit

@Suppress("unused")
class AppListener : ApplicationListener {
    override fun dispose() {
        Events.fire(ServerExit())
    }
}
