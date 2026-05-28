package io.github.pylonmc.rebar.item.base

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemListener
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Allows the item to act as a normal vanilla fuel.
 *
 * For example, by default, a 'magic coal' item which has a material of coal cannot
 * be burnt in furnaces. However, if your magic coal item implements this interface,
 * it will be treated the same as a normal piece of coal when burnt as fuel.
 */
interface VanillaFurnaceFuel