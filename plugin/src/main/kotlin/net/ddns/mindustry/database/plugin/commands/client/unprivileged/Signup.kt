package net.ddns.mindustry.database.plugin.commands.client.unprivileged

import arc.util.CommandHandler
import arc.util.Log
import mindustry.gen.Call
import mindustry.gen.Player
import net.ddns.mindustry.database.client.AccountQueries.SignupStatus.*
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configAccountLimit
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configSessionDuration
import java.time.Duration

class Signup(handler: CommandHandler) : UnprivilegedClientCommand(handler) {
    companion object {
        init {
            description = "Creates a new account."
            parameters = ""
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        textInputHandler.addTextInput(
            "[gold]Signup (1/2)",
            "Type in the username you'll use for your account (this [scarlet]cannot[] be changed)",
            ::callbackSignupUsername,
            32
        ).show()
    }

    private fun callbackSignupUsername(player: Player, text: String?, args: Array<String>) {
        if (text == null) {
            Log.warn("Cancelling signup at username step.")
            return
        }

        playerToUsernameMap[player] = text

        textInputHandler.addTextInput(
            "[gold]Signup (2/2)",
            "Type in the password you wish to use",
            ::callbackSignupPassword
        ).show()
    }

    private fun callbackSignupPassword(player: Player, text: String?, args: Array<String>) {
        if (text == null) {
            Log.warn("Cancelling signup at password step.")
            return
        }

        val username = playerToUsernameMap[player]

        playerToUsernameMap.remove(player)
        playerToDisplayName.remove(player)

        val signupStatus = database!!.account().signup(username!!, text.toCharArray(), player.ip(), player.uuid(),
            Duration.ofHours(configSessionDuration.num().toLong()), configAccountLimit.num()) // TODO Move this into the SecurityConfig.

        when (signupStatus) {
            is UsernameInUse -> {
                Call.infoMessage(player.con(), "[scarlet]The username that was provided is already in use.")
            }

            is InvalidUsername, is InvalidPassword -> {
                Call.infoMessage(player.con(), "[scarlet]The username or password that was provided is invalid.")
            }

            is Created -> {
                player.sendMessage("Signup was successful. You can now log in with the [gold]/login[] command.")
            }

            is LimitReached -> {
                player.sendMessage("[scarlet]You cannot create any more accounts.")
            }
        }
    }
}