package net.ddns.mindustry.database.plugin

import arc.math.geom.Point2
import mindustry.net.Administration.ActionType

data class TileEvent(
    val actorName: String,
    val accountId: Int,
    // The block's bottom-left origin tile
    val coord: Int,
    // Every tile the block covers (packed), so a lookup on any covered tile finds this event.
    val coords: IntArray,
    val action: ActionType,
    val blockName: String?,
    val configSummary: String?,
    val rotation: Int,
    val epochMillis: Long,
)

/**
 * In-memory log of player tile actions, with a configurable capacity.
 */
object TileHistoryStore {
    private val events = ArrayDeque<TileEvent>()

    // Last signature to prevent recording duplicate consecutive events
    private val lastSignature = HashMap<Int, String>()

    var capacity = 10_000

    fun record(event: TileEvent) {
        val signature = signatureOf(event)
        if (lastSignature[event.coord] == signature) {
            return
        }
        lastSignature[event.coord] = signature

        events.addLast(event)
        while (events.size > capacity) {
            events.removeFirst()
        }
    }

    fun get(coord: Int): List<TileEvent> = events.filter { coord in it.coords }

    fun getByActor(name: String): List<TileEvent> =
        events.filter { it.actorName.equals(name, ignoreCase = true) }

    fun clear() {
        events.clear()
        lastSignature.clear()
    }

    private fun signatureOf(event: TileEvent) =
        "${event.action}|${event.blockName}|${event.configSummary}|${event.rotation}|${event.accountId}"
}

fun renderTileHistory(x: Int, y: Int): String {
    val history = TileHistoryStore.get(Point2.pack(x, y))

    if (history.isEmpty()) {
        return "[accent]No history for ($x, $y)."
    }

    val builder = StringBuilder("[accent]History for ($x, $y):[]")
    for (event in history) {
        builder.append("\n  [white]").append(event.actorName).append("[white] ").append(describe(event))
            .append(" — ").append(relativeTime(event.epochMillis))
    }
    return builder.toString()
}

fun renderPlayerHistory(name: String): String {
    val history = TileHistoryStore.getByActor(name)

    if (history.isEmpty()) {
        return "[accent]No history for [white]$name[accent]."
    }

    val shown = history.takeLast(50)
    val builder = StringBuilder("[accent]History for [white]$name[accent]:[]")
    if (history.size > shown.size) {
        builder.append("\n  [gray]… ${history.size - shown.size} older entries not shown")
    }
    for (event in shown) {
        builder.append("\n  ").append(describe(event))
            .append(" at (${Point2.x(event.coord)}, ${Point2.y(event.coord)})")
            .append("[white] — ").append(relativeTime(event.epochMillis))
    }
    return builder.toString()
}

private fun describe(event: TileEvent): String {
    val block = event.blockName ?: "block"
    return when (event.action) {
        ActionType.placeBlock -> "[green]placed[] $block"
        ActionType.breakBlock -> "[scarlet]broke[] $block"
        ActionType.rotate -> "[sky]rotated[] $block"
        ActionType.configure -> "[orange]configured[] $block" + (event.configSummary?.let { " to $it" } ?: "")
        else -> "[lightgray]${event.action.name}[] $block"
    }
}

private fun relativeTime(epochMillis: Long): String {
    val seconds = (System.currentTimeMillis() - epochMillis) / 1000
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}
