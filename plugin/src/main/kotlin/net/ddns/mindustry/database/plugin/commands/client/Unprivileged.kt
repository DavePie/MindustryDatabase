package net.ddns.mindustry.database.plugin.commands.client

import arc.util.CommandHandler
import arc.util.Log
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Player
import net.ddns.mindustry.database.client.AccountQueries
import net.ddns.mindustry.database.client.AccountQueries.LoginStatus.*
import net.ddns.mindustry.database.client.AccountQueries.SignupStatus.*
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.database.plugin.configSessionDuration
import net.ddns.mindustry.segment.TextInputHandler

private var playerToUsernameMap: MutableMap<Player, String> = mutableMapOf()
private var playerToDisplayName: MutableMap<Player, String> = mutableMapOf()

private var textInputHandler: TextInputHandler = TextInputHandler()

fun loadUnprivilegedCommands(handler: CommandHandler) {
    handler.register("login", "Logs you into your account.", ::login)
    handler.register("signup", "Creates a new account.", ::signup)
    handler.register("logout", "Logs you out of your current account.", ::logout)

    handler.register("change-display-name", "Changes your display name.", ::changeDisplayName)
    handler.register("change-password", "Changes your password.", ::changePassword)
}

private fun login(args: Array<String>, player: Player) {
    textInputHandler.addTextInput(
        "[gold]Login (1/2)",
        "Type in your username",
        ::callbackLoginUsername,
        32,
        "",
        false
    ).show()
}

private fun callbackLoginUsername(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling login at username step.")
        return
    }

    playerToUsernameMap[player] = text

    textInputHandler.addTextInput(
        "[gold]Login (2/2)",
        "Type in your password",
        ::callbackLoginPassword
    ).show()
}

private fun callbackLoginPassword(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling login at password step.")
        playerToUsernameMap.remove(player)
        return
    }

    val username = playerToUsernameMap[player]
    playerToUsernameMap.remove(player)

    val loginResult = database!!.auth().login(
        username, text.toCharArray(), player.ip(),
        player.uuid(), configSessionDuration.num()
    )

    when (loginResult) {
        is WrongCredentials -> Call.infoMessage(player.con(), "[scarlet]The credentials provided were " +
                "invalid. Please ensure that you've entered the correct credentials and that you've signed up.")
        is AlreadyLoggedIn -> Call.infoMessage(player.con(), "[orange]Already signed in.")
        is LoggedIn -> {
            val displayName = loginResult.account.displayName

            player.team(Team.sharded)
            player.name(displayName)
            player.sendMessage("Logged in successfully.")
        }
        // The else is not needed. This should throw an error if all case aren't accounted for.
//        else -> {
//            player.sendMessage("An unknown status was received. Please contact a staff member of this server.")
//            Log.warn("The login method returned a status that is not accounted for.")
//            Log.warn("Returned status: ${loginResult.javaClass.name}")
//        }
    }
}



private fun signup(args: Array<String>, player: Player) {
    textInputHandler.addTextInput(
        "[gold]Signup (1/3)",
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
        "[gold]Signup (2/3)",
        "Type in the display name you wish to use (this can be changed later)",
        ::callbackSignupDisplayName
    ).show()
}

private fun callbackSignupDisplayName(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling signup at display name step.")
        return
    }

    playerToDisplayName[player] = text

    textInputHandler.addTextInput(
        "[gold]Signup (3/3)",
        "Type in the password you'll use for your account (this will not be verified)",
        ::callbackSignupPassword
    ).show()
}

private fun callbackSignupPassword(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling signup at password step.")
        return
    }

    val username = playerToUsernameMap[player]
    val displayName = playerToDisplayName[player]

    playerToUsernameMap.remove(player)
    playerToDisplayName.remove(player)

    val signupStatus = database!!.auth().signup(username, text.toCharArray(), displayName, player.ip(), player.uuid())

    when (signupStatus) {
        is UsernameInUse -> {
            Call.infoMessage(player.con(), "[scarlet]The username that was provided is already in use.")
        }

        is InvalidName, is InvalidPassword -> {
            Call.infoMessage(player.con(), "[scarlet]The username or password that was provided is invalid.")
        }

        is Created -> {
            player.sendMessage("Signup was successful. You can now log in with the [gold]/login[] command.")
        }
    }
}



private fun logout(args: Array<String>, player: Player) {
    val account = database!!.auth().find(player.ip(), player.uuid())

    if (account.isEmpty) {
        player.sendMessage("[orange]There is either no active session or you're not logged in.")
        return
    }

    database!!.auth().logout(account.get())
    player.team(Team.derelict)
    player.unit().kill()
    player.sendMessage("Logged out successfully.")
}



private fun changeDisplayName(args: Array<String>, player: Player) {
    textInputHandler.addTextInput(
        "[gold]Change display name (1/2)",
        "Type in the new display name you wish to use",
        ::callbackChangeDisplayName
    ).show()
}

private fun callbackChangeDisplayName(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling display name change at display name step.")
        return
    }

    val account = database!!.auth().find(player.ip(), player.uuid())

    if (account.isEmpty) {
        Call.infoMessage(player.con(), "[orange]There is either no active session or you're not logged in.")
        return
    }

    database!!.auth().updateDisplayName(account.get(), text)
    player.name(text)
    player.sendMessage("Changed display name successfully.")
}



private fun changePassword(args: Array<String>, player: Player) {
    textInputHandler.addTextInput(
        "[gold]Change password (1/2)",
        "Type in your current password",
        ::callbackChangePasswordCurrentPassword
    ).show()
}

private fun callbackChangePasswordCurrentPassword(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling password change at current password step.")
        return
    }

    textInputHandler.addTextInput(
        "[gold]Change password (2/2)",
        "Type in the new password",
        ::callbackChangePasswordNewPassword,
        args = arrayOf(text)
    ).show()
}

private fun callbackChangePasswordNewPassword(player: Player, text: String?, args: Array<String>) {
    if (text == null) {
        Log.warn("Cancelling password change at password step.")
        return
    }

    val oldPassword = args[0]
    val account = database!!.auth().find(player.ip(), player.uuid())

    if (account.isEmpty) {
        Call.infoMessage(player.con(), "[orange]There is either no active session or you're not logged in.")
        return
    }

    val result = database!!.auth().updatePassword(account.get(), oldPassword.toCharArray(), text.toCharArray())

    when (result) {
        is AccountQueries.PasswordUpdateStatus.WrongPassword -> Call.infoMessage(player.con(), "[scarlet]" +
                "Wrong password. Ensure that you've typed in the right password.")
        is AccountQueries.PasswordUpdateStatus.InvalidPassword -> Call.infoMessage(player.con(), "[scarlet]" +
                "The new password is invalid and does not meet security requirements.")
        is AccountQueries.PasswordUpdateStatus.Updated -> player.sendMessage("Password changed successfully.")
    }
}
