package io.github.pylonmc.rebar.waila

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.entity.RebarEntity
import org.bukkit.entity.Player

@FunctionalInterface
fun interface WailaSupplier {
    fun getWaila(player: Player): WailaDisplay?
}

class RebarBlockWailaSupplier(
    val source: RebarBlock
) : WailaSupplier {
    override fun getWaila(player: Player): WailaDisplay? = source.getWaila(player)
}

class RebarEntityWailaSupplier(
    val source: RebarEntity<*>
) : WailaSupplier {
    override fun getWaila(player: Player): WailaDisplay? = source.getWaila(player)
}