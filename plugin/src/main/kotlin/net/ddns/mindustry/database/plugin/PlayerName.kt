package net.ddns.mindustry.database.plugin

import arc.util.Log
import mindustry.gen.Player

/**
 * Stores a player's displayed name.
 *
 * A player's name is composed from three independent inputs:
 *   - [State.base]   the player's real chosen name, captured once on connect.
 *   - [State.tag]    an optional role symbol, shown as a `<symbol>` prefix.
 *   - [State.hidden] whether the name is hidden (unauthenticated or banned).
 */
object PlayerName {

    private data class State(val base: String, var tag: String? = null, var hidden: Boolean = false)

    private val states: MutableMap<String, State> = mutableMapOf()

    fun capture(player: Player) {
        states.getOrPut(player.uuid()) { State(player.name()) }
    }

    fun setTag(player: Player, symbol: String?) {
        val state = state(player) ?: return
        state.tag = symbol
        render(player, state)
    }

    fun hide(player: Player) {
        val state = state(player) ?: return
        state.hidden = true
        render(player, state)
    }

    fun show(player: Player) {
        val state = state(player) ?: return
        state.hidden = false
        render(player, state)
    }

    fun isHidden(player: Player): Boolean = states[player.uuid()]?.hidden ?: false

    fun forget(player: Player) {
        states.remove(player.uuid())
    }

    private fun state(player: Player): State? {
        val state = states[player.uuid()]
        if (state == null) {
            Log.warn("No tracked name state for @. Did capture() run on connect?", player.uuid())
        }
        return state
    }

    private fun render(player: Player, state: State) {
        player.name(
            when {
                state.hidden -> ""
                state.tag != null -> "[accent]<[white]${state.tag}[accent]>[white] ${state.base}"
                else -> state.base
            }
        )
    }
}
