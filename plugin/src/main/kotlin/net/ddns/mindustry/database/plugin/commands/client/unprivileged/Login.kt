package net.ddns.mindustry.database.plugin.commands.client.unprivileged

import arc.Events
import arc.util.CommandHandler
import arc.util.Log
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Player
import net.ddns.mindustry.database.client.AccountQueries.LoginStatus.*
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configSessionDuration
import net.ddns.mindustry.database.plugin.events.PlayerLogin
import net.ddns.mindustry.segment.ui.Child
import net.ddns.mindustry.segment.ui.textInput.BaseTextInput
import java.time.Duration

class Login(handler: CommandHandler) : UnprivilegedClientCommand(handler) {
    companion object {
        init {
            description = "Logs you into your account."
            parameters = ""
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        textInputHandler.addTextInput(
            "[gold]Login (1/2)",
            "Type in your username",
            ::callbackLoginUsername,
            32,
            "",
            false
        ).show(player.con())
    }

    private fun callbackLoginUsername(player: Player, child: Child) {
        if (child !is BaseTextInput) { return }

        if (child.text == null) {
            Log.warn("Cancelling login at username step.")
            return
        }

        playerToUsernameMap[player] = child.text!!

        textInputHandler.addTextInput(
            "[gold]Login (2/2)",
            "Type in your password",
            ::callbackLoginPassword
        ).show(player.con())
    }

    private fun callbackLoginPassword(player: Player, child: Child) {
        if (child !is BaseTextInput) { return }

        if (child.text == null) {
            Log.warn("Cancelling login at password step.")
            playerToUsernameMap.remove(player)
            return
        }

        val username = playerToUsernameMap[player]
        playerToUsernameMap.remove(player)

        val loginResult = database!!.account().login(
            username!!, child.text!!.toCharArray(), player.ip(),
            player.uuid(), Duration.ofHours(configSessionDuration.num().toLong())
        )

        when (loginResult) {
            is WrongCredentials -> Call.infoMessage(player.con(), "[scarlet]The credentials provided were " +
                    "invalid. Please ensure that you've entered the correct credentials and that you've signed up.")
            is AlreadyLoggedIn -> Call.infoMessage(player.con(), "[orange]Already signed in.")
            is LoggedIn -> {
                player.team(Team.sharded)
                player.sendMessage("Logged in successfully.")
                Events.fire(PlayerLogin(player, loginResult.account))
            }
            // The else is not needed. This should throw an error if all case aren't accounted for.
//        else -> {
//            player.sendMessage("An unknown status was received. Please contact a staff member of this server.")
//            Log.warn("The login method returned a status that is not accounted for.")
//            Log.warn("Returned status: ${loginResult.javaClass.name}")
//        }
        }
    }
}