package net.ddns.mindustry.database.plugin.commands.client

import arc.util.CommandHandler
import mindustry.gen.Player

abstract class ClientCommand(handler: CommandHandler) {
    companion object {
        var description: String = ""
        var parameters: String = ""
    }

    init {
        val commandName = kebab(this::class.simpleName!!)

        if (parameters == "") {
            handler.register(commandName, description, ::runner)
        } else {
            handler.register(commandName, parameters, description, ::runner)
        }
    }

    /**
     * Turns a string into a kebab. a-kebab-string.
     *
     * @param toKebab The string to turn into a kebab. **Must** be a camelCase or PascalCase string for the function to
     * work as expected.
     */
    private fun kebab(toKebab: String): String {
        var kebabString = ""

        for (character in toKebab) {
            if (character.isUpperCase() && character != toKebab.first()) {
                kebabString += String.format("-%c", character.lowercaseChar())
                continue
            }

            kebabString += character.lowercaseChar()
        }

        return kebabString
    }

    abstract fun runner(arguments: Array<String>, player: Player)
}
