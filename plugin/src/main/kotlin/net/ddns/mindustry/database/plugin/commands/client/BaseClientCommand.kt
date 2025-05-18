package net.ddns.mindustry.database.plugin.commands.client

import arc.util.CommandHandler
import mindustry.gen.Player
import net.ddns.mindustry.database.plugin.commands.BaseCommand

abstract class BaseClientCommand(handler: CommandHandler) : BaseCommand(handler) {
    init {
        val commandName = kebab(this::class.simpleName!!)

        if (parameters == "") {
            handler.register(commandName, description) { arguments: Array<String>, player: Player ->
                runner(arguments, player) }
        } else {
            handler.register(commandName, parameters, description) { arguments: Array<String>, player: Player ->
                runner(arguments, player) }
        }
    }

    abstract fun runner(arguments: Array<String>, player: Player)

    override fun runner(arguments: Array<String>) {
        throw RuntimeException("Ran server command runner, but command is a client command.")
    }
}
