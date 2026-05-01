package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.item.RebarItem
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Represents a [RebarItem] that has special behavior when a player rejoins the server.
 * This interface allows items to perform actions or update their state when a player reconnects.
 */
interface RebarRejoinHandler {
    /**
     * This is only called for players who have played before (not first-time players).
     */
    fun onRejoin(event: PlayerJoinEvent)

    companion object : Listener {
        @EventHandler
        fun onRejoin(event : PlayerJoinEvent) {
            if (event.player.firstPlayed == 0L) {
                return
            }

            for (item in event.player.inventory) {
                val rebarItem = RebarItem.fromStack(item)
                if (rebarItem is RebarRejoinHandler) {
                    rebarItem.onRejoin(event)
                }
            }
        }
    }
}