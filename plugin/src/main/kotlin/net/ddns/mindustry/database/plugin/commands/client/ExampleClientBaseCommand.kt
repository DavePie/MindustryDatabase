package net.ddns.mindustry.database.plugin.commands.client

import arc.util.CommandHandler
import mindustry.gen.Player

@Deprecated("This is not intended for production usage..")
class ExampleClientBaseCommand(handler: CommandHandler) : BaseClientCommand(handler) {
    companion object {
        init {
            description = "Lorem ipsum."
            parameters = "<text>"
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        player.sendMessage(arguments[0])
    }
}