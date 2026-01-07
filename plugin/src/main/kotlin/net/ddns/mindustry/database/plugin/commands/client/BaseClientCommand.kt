package net.ddns.mindustry.database.plugin.commands.client

import arc.util.CommandHandler
import arc.util.Log
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.net.Packets
import net.ddns.mindustry.database.plugin.commands.BaseCommand
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configCommandRateLimit
import java.time.Instant

abstract class BaseClientCommand(handler: CommandHandler) : BaseCommand(handler) {
    companion object {
        var playerLastAttempts: PlayerCommandAttempts = PlayerCommandAttempts()
    }

    init {
        val commandName = kebab(this::class.simpleName!!)

        if (parameters.isEmpty()) {
            handler.register(commandName, description) { arguments: Array<String>, player: Player ->
                clientRunner(arguments, player) }
        } else {
            handler.register(commandName, parameters, description) { arguments: Array<String>, player: Player ->
                clientRunner(arguments, player) }
        }
    }

    /** Purges offline players to ensure memory usage isn't too much */
//    private fun purgeOffline() {
//        for (uuid in playerLastAttempts.keys) {
//            if (Groups.player.contains() {player -> player.uuid() == uuid}) continue
//            playerLastAttempts.remove(uuid)
//        }
//    }

    private fun clientRunner(arguments: Array<String>, player: Player) {
//        purgeOffline()
//        val now = Instant.now()
//
//        if (!playerLastAttempts.contains(player.uuid())) {
//            runner(arguments, player)
//            val attempt = CommandAttempt(now, now, Long.MAX_VALUE)
//            playerLastAttempts[player.uuid()] = mutableListOf(attempt)
//            return
//        }
//
//        val lastAttempt = playerLastAttempts[player.uuid()]!!
//
//        //TODO: replace with config
//        if ((now.epochSecond - lastAttempt.epochSecond) <= configCommandRateLimit.num()) {
//            Log.info("Kicking @ for potentially spamming commands maliciously.", player.uuid())
//            player.kick(Packets.KickReason.serverClose, 1000 * 60 * 60)
//            return
//        }
        playerLastAttempts.purgeOffline()
        playerLastAttempts.addPlayer(player.uuid())
        playerLastAttempts.addAttempt(player.uuid())

        if (playerLastAttempts.isSpamming(player.uuid())) {
            Log.info("Kicking @ for spamming commands in a potentially malicious manner.", player.uuid())
            player.kick(Packets.KickReason.serverClose, 1000 * 60 * 60) // 60 minutes
            return
        }

        runner(arguments, player)
    }

    abstract fun runner(arguments: Array<String>, player: Player)

    override fun runner(arguments: Array<String>) {
        throw RuntimeException("Ran server command runner, but command is a client command.")
    }
}
