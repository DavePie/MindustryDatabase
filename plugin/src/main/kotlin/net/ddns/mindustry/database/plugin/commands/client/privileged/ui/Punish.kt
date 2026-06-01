package net.ddns.mindustry.database.plugin.commands.client.privileged.ui

import arc.util.CommandHandler
import arc.util.Log
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import net.ddns.mindustry.database.plugin.Main.Companion.database
import net.ddns.mindustry.segment.menuHandler
import net.ddns.mindustry.segment.textInputHandler
import net.ddns.mindustry.segment.ui.Child
import net.ddns.mindustry.segment.ui.menu.BaseMenu
import net.ddns.mindustry.segment.ui.textInput.BaseTextInput
import kotlin.time.Duration
import kotlin.collections.getOrDefault
import kotlin.time.toJavaDuration

class Punish(handler: CommandHandler) : PrivilegedUiClientCommand(handler) {
    val optionsMenu = menuHandler.addMenu("[gold]Punish UI", "Select an action.", punishOptions, ::typeSelected, true)
    var playerMenu: BaseMenu? = null
    val reasonInput = textInputHandler.addTextInput("[gold]Punish UI", "Why are you taking action against this person?",
        ::reasonGiven, 200, persist=true) // 200 too much?
    val durationInput = textInputHandler.addTextInput("[gold]Punish UI", "How long should the ban last? (ex: 365d)",
        ::gotDuration, 5, persist=true) // any more than 5 is absurd.

    companion object {
//        val banPermission: Permission
        val punishmentBuilders: MutableMap<Player, PunishmentBuilder> = mutableMapOf()
        val punishOptions = arrayOf(arrayOf("Ban", "Kick", "Warn"))

        init {
            description = "Punish a player via UI."
            parameters = ""

//            database!!.role().newPermission("ban")
//            banPermission = database!!.role().findPermission("ban").get()
        }
    }

    fun confirmPermissions(player: Player): Boolean {
        var score = 0   // player needs at least one permission to run the command; if score is non-zero, then they can
                        // run the command.

        for (punishment in punishOptions[0]) {
            val permission = database!!.role().findPermission(punishment.lowercase())
            if (permission.isEmpty) {
                Log.err("Couldn't find permission for @.", punishment.lowercase())
                player.sendMessage("[scarlet]Couldn't confirm your permissions.")
                return false
            }

            if (hasPermission(permission.get(), player) != null) { score += 1 }
        }

        return score != 0
    }

    fun typeError(player: Player, expected: String = "BaseMenu") {
        player.sendMessage("[scarlet]A fatal error occurred.")
        Log.err("Menu callback expected @ for child type, but got a different type instead.", expected)
    }

    fun retrieveBuilder(player: Player): PunishmentBuilder? {
        val builder = punishmentBuilders.getOrDefault(player, null)
        if (builder == null) {
            player.sendMessage("[scarlet]Couldn't retrieve the punishment builder.")
            Log.err("Couldn't retrieve the punishment builder for @ / @.", player.plainName(), player.uuid())
        }

        return builder
    }

    override fun runner(arguments: Array<String>, player: Player) {
        if (!confirmPermissions(player)) { return }

        optionsMenu.show(player.con())
    }

    fun typeSelected(player: Player, child: Child) {
        if (child !is BaseMenu) {
            typeError(player)
            return
        }
        else if (child.option >= punishOptions[0].size) { return }
        else if (child.option == -1) { return }

        val punishmentType = punishOptions[0][child.option].lowercase();
        val permission = database!!.role().findPermission(punishmentType)
        if (permission.isEmpty) {
            Log.err("Couldn't find permission for @.", punishmentType)
            player.sendMessage("[scarlet]Couldn't confirm your permissions.")
            return;
        }
        if (hasPermission(permission.get(), player) == null) {
            optionsMenu.show(player.con())
            return
        }

        val builder = PunishmentBuilder(optionsMenu.id, player)
        builder.punishmentType = punishmentType
        punishmentBuilders[player] = builder

        val usernameList = mutableListOf<Array<String>>()
        for (username in builder.players) {
            usernameList.addFirst(arrayOf(username.value))
        }

        playerMenu = menuHandler.addMenu("[gold]Punish UI", "Select a player.", usernameList.toTypedArray(),
            ::playerSelected, false)
//        playerMenu!!.id = builder.menuId
        playerMenu!!.show(player.con())
    }

    fun playerSelected(player: Player, child: Child) {
        if (child !is BaseMenu) {
            typeError(player)
            return
        }
        else if (child.option == -1) { return }

        val builder = retrieveBuilder(player) ?: return
        if (child.option >= builder.players.size) { return }

        val playerArray = builder.players.keys.toTypedArray()
        playerArray.reverse()

        val target = child.option
        val targetPlayer = playerArray.getOrNull(target)
        if (targetPlayer == null) {
            player.sendMessage("[scarlet]Couldn't find the specified player!")
            Log.err("Couldn't find target player for @ / @.", player.plainName(), player.uuid())
            return
        }

        val targetAccount = database!!.account().find(targetPlayer.ip(), targetPlayer.uuid())
        if (targetAccount.isEmpty) {
            player.sendMessage("[scarlet]Couldn't find an account for that player! Are they logged in?")
            return
        }
        builder.target = targetAccount.get()

        reasonInput.show(player.con())
    }

    fun reasonGiven(player: Player, child: Child) {
        if (child !is BaseTextInput) {
            typeError(player, "BaseTextInput")
            return
        }
        else if (child.text.isNullOrEmpty()) {
            if (child.text != null) {
                Call.infoMessage("[yellow]Cannot have an empty reason.")
                reasonInput.show()
            }
            return
        }

        val builder = retrieveBuilder(player) ?: return
        builder.reason = child.text

        if (builder.punishmentType.isNullOrEmpty()) {
            player.sendMessage("[scarlet]Punishment type was empty.")
            Log.err("Punishment type was empty for @ / @.", player.plainName(), player.uuid())
            return
        }

        if (builder.punishmentType!!.lowercase() == "ban") {
            durationInput.show(player.con())
            return
        }

        builder.execute()
    }

    fun gotDuration(player: Player, child: Child) {
        if (child !is BaseTextInput) {
            typeError(player, "BaseTextInput")
            return
        }
        else if (child.text.isNullOrEmpty()) {
            if (child.text != null) {
                Call.infoMessage("[yellow]Cannot have an empty duration.")
                reasonInput.show()
            }
            return
        }

        val builder = retrieveBuilder(player) ?: return

        val duration: Duration
        try {
            duration = Duration.parse(child.text!!)
        } catch (_: IllegalArgumentException) {
            Call.infoMessage(player.con(), "[scarlet]Must have a valid duration.")
            durationInput.show()
            return
        }

        builder.duration = duration.toJavaDuration()
        builder.execute()
    }
}