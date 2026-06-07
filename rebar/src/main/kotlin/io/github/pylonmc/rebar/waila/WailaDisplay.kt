package io.github.pylonmc.rebar.waila

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.util.breakProgress
import io.github.pylonmc.rebar.util.getPreferredTool
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.`object`.ObjectContents
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Player

/**
 * The configuration for a WAILA bossbar (the bar shown at the top of your
 * screen when looking at a block).
 */
class WailaDisplay @JvmOverloads constructor(
    var text: Component,
    var color: BossBar.Color = RebarConfig.WailaConfig.DEFAULT_DISPLAY.color,
    var overlay: BossBar.Overlay = RebarConfig.WailaConfig.DEFAULT_DISPLAY.overlay,
    var progress: Float = RebarConfig.WailaConfig.DEFAULT_DISPLAY.progress
) {

    fun color(color: BossBar.Color) = apply { this.color = color }
    fun overlay(overlay: BossBar.Overlay) = apply { this.overlay = overlay }
    fun progress(progress: Float) = apply { this.progress = progress }

    fun separator() = apply {
        text = text.append(seperator)
    }

    fun add(componentLike: ComponentLike) = apply {
        text = text.append(seperator).append(componentLike)
    }

    fun addWithoutSeperator(componentLike: ComponentLike) = apply {
        text = text.append(componentLike)
    }

    companion object {

        val seperator = Component.text(" | ")
            .color(TextColor.fromHexString("#b2b2b2"))

        @JvmSynthetic
        internal fun getWailaBlockPrefix(block: Block, player: Player): Component? {
            val preferredTool = block.type.getPreferredTool() ?: return null
            val canPlayerBreak = block.isPreferredTool(player.inventory.itemInMainHand)
            var root = Component.empty()
            if (canPlayerBreak) {
                root = root.append(Component.text("+").color(TextColor.color(0, 255, 0)))
            } else {
                root = root.append(Component.text("-").color(TextColor.color(255, 0, 0)))
            }
            root = root.append(Component.`object`(ObjectContents.sprite(
                NamespacedKey("minecraft", "items"),
                NamespacedKey("minecraft", "item/" + preferredTool.key.key),
            )))
            return root
        }

        /**
         * Constructs a new WAILA builder which begins with the given [component].
         */
        @JvmStatic
        fun of(component: Component) = WailaDisplay(component)

        /**
         * Constructs a new WAILA builder which begins with the name of the provided [block] and
         * has progress equal to the break progress of the block.
         */
        @JvmStatic
        fun of(block: RebarBlock, player: Player): WailaDisplay {
            val name = Component.translatable("${block.key.namespace}.item.${block.key.key}.name")
            val prefix = getWailaBlockPrefix(block.block, player)
            val display = if (prefix != null) {
                of(prefix).add(name)
            } else {
                of(name)
            }
            return display.progress(1.0F - block.block.breakProgress)
        }
    }
}