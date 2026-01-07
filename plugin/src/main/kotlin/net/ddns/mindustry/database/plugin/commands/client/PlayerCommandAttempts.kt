package net.ddns.mindustry.database.plugin.commands.client

import arc.util.Log
import mindustry.gen.Groups
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configCommandAttempts
import net.ddns.mindustry.database.plugin.configs.PluginConfigs.Configs.configCommandRateLimit
import java.time.Instant
import java.time.temporal.ChronoUnit

class PlayerCommandAttempts {
    private val playerLastAttempts: MutableMap<String, MutableList<CommandAttempt>> = mutableMapOf()

    fun isSpamming(uuid: String): Boolean {
        val attempts = getPlayerAttempts(uuid)

        if (attempts.isNullOrEmpty()) return false;

        Log.debug("Average time: @", getAverageDuration(attempts))
        return getAverageDuration(attempts) < configCommandRateLimit.num()
    }

    fun getPlayerAttempts(uuid: String): MutableList<CommandAttempt>? {
        if (!playerLastAttempts.contains(uuid)) return null

        return playerLastAttempts[uuid]
    }

    fun getAverageDuration(attempts: MutableList<CommandAttempt>): Long {
        val total = attempts.sumOf { a -> a.duration }
        return total / attempts.size
    }

    fun addPlayer(uuid: String) {
        if (playerLastAttempts.contains(uuid)) return

        playerLastAttempts[uuid] = mutableListOf()
    }

    fun addAttempt(uuid: String) {
        val now = Instant.now()
        val attempts = getPlayerAttempts(uuid)!!
        val previousAttempt = attempts.lastOrNull()
        val previousTime: Instant

        if (previousAttempt == null) previousTime = Instant.now().minusSeconds(configCommandRateLimit.num().toLong() * 2)
        else previousTime = previousAttempt.attemptTime

        Log.debug("Previous time: @ seconds.", previousTime.until(now, ChronoUnit.SECONDS))

        attempts.add(CommandAttempt(previousTime, now, previousTime.until(now, ChronoUnit.SECONDS)))
        if (attempts.size > configCommandAttempts.num()) attempts.removeFirst()
    }

    fun purgeOffline() {
        for (uuid in playerLastAttempts.keys) {
            if (Groups.player.contains() { player -> player.uuid() == uuid}) continue
            playerLastAttempts.remove(uuid)
        }
    }
}