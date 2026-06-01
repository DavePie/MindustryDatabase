package net.ddns.mindustry.database.plugin.commands.client.unprivileged

import arc.util.CommandHandler
import arc.util.Log
import mindustry.gen.Call
import mindustry.gen.Player
import net.ddns.mindustry.database.client.AccountQueries
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.segment.textInputHandler
import net.ddns.mindustry.segment.ui.Child
import net.ddns.mindustry.segment.ui.textInput.BaseTextInput

class ChangePassword(handler: CommandHandler) : UnprivilegedClientCommand(handler) {
    companion object {
        init {
            description = "Changes your password."
            parameters = ""
        }
    }

    override fun runner(arguments: Array<String>, player: Player) {
        textInputHandler.addTextInput(
            "[gold]Change password (1/2)",
            "Type in your current password",
            ::callbackChangePasswordCurrentPassword
        ).show()
    }

    private fun callbackChangePasswordCurrentPassword(player: Player, child: Child) {
        if (child !is BaseTextInput) { return }

        if (child.text == null) {
            Log.warn("Cancelling password change at current password step.")
            return
        }

        val textInput = textInputHandler.addTextInput(
            "[gold]Change password (2/2)",
            "Type in the new password",
            ::callbackChangePasswordNewPassword,
        )
        textInput.args[0] = child.text!!
        textInput.show()
    }

    private fun callbackChangePasswordNewPassword(player: Player, child: Child) {
        if (child !is BaseTextInput) { return }

        if (child.text == null) {
            Log.warn("Cancelling password change at password step.")
            return
        }

        val oldPassword = child.args[0]
        val account = database!!.account().find(player.ip(), player.uuid())

        if (account.isEmpty) {
            Call.infoMessage(player.con(), "[orange]There is either no active session or you're not logged in.")
            return
        }

        val result = database!!.account().updatePassword(account.get(), oldPassword.toCharArray(), child.text!!.toCharArray())

        when (result) {
            is AccountQueries.PasswordUpdateStatus.WrongPassword -> Call.infoMessage(player.con(), "[scarlet]" +
                    "Wrong password. Ensure that you've typed in the right password.")
            is AccountQueries.PasswordUpdateStatus.InvalidPassword -> Call.infoMessage(player.con(), "[scarlet]" +
                    "The new password is invalid and does not meet security requirements.")
            is AccountQueries.PasswordUpdateStatus.Updated -> player.sendMessage("Password changed successfully.")
        }
    }
}