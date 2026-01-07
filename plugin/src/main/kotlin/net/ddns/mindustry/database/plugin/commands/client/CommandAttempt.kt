package net.ddns.mindustry.database.plugin.commands.client

import java.time.Instant

class CommandAttempt(public val lastAttemptTime: Instant, public val attemptTime: Instant, public val duration: Long)
