package io.github.pylonmc.rebar.util

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color
import kotlin.math.roundToInt

class ProgressBar : ComponentLike {

    var proportion: Double? = null
    var bars: Int = 20
    var barColor: TextColor = TextColor.fromHexString("#ffffff")!!
    var emptyColor: TextColor = TextColor.fromHexString("#4d4d4d")!!
    var barString = "|"
    var prefix: ComponentLike = Component.empty()
    var suffix: ComponentLike = Component.empty()

    fun bars(bars: Int) = apply { this.bars = bars }

    fun proportion(proportion: Double) = apply {
        check(proportion > -1.0e-6) { "Proportion must be greater than zero but was $proportion" }
        check(proportion < 1.0 + 1.0e-6) { "Proportion must be less than one but was $proportion" }
        this.proportion = proportion.coerceIn(0.0, 1.0)
    }

    fun barColor(barColor: TextColor) = apply { this.barColor = barColor }
    fun barColor(barColor: Color) = apply { this.barColor = barColor.toTextColor() }
    fun barColor(fluid: RebarFluid) = apply { this.barColor = fluid.color }

    fun emptyColor(emptyColor: TextColor) = apply { this.emptyColor = emptyColor }
    fun emptyColor(emptyColor: Color) = apply { this.emptyColor = emptyColor.toTextColor() }

    fun barString(barString: String) = apply { this.barString = barString }

    fun prefix(component: ComponentLike) = apply { this.prefix = component }
    fun suffix(component: ComponentLike) = apply { this.suffix = component }

    override fun asComponent(): Component {
        check(proportion != null) { "Progress bar did not have its proportion set; did you forget to call `.proportion(...)`?" }
        val filledBars = (bars * proportion!!).roundToInt()

        return Component.empty()
            .append(prefix)
            .append(Component.text(barString.repeat(filledBars)).color(barColor))
            .append(Component.text(barString.repeat(bars - filledBars)).color(emptyColor))
            .append(suffix)
    }

    companion object {

        @JvmStatic
        fun recipeProgress(progress: Double) = ProgressBar()
            .barColor(NamedTextColor.WHITE)
            .proportion(progress)
            .suffix(Component.text(" ")
                .append(UnitFormat.PERCENT.format(progress * 100).decimalPlaces(0))
            )

        @JvmStatic
        fun fuelRemaining(total: Double, remaining: Double) = ProgressBar()
            .barColor(TextColor.fromHexString("#f9802f")!!)
            .proportion(remaining / total)
            .suffix(Component.text(" ")
                .append(UnitFormat.SECONDS.format(remaining))
            )

        @JvmStatic
        fun fluidContents(fluid: RebarFluid, capacity: Double, amount: Double) = ProgressBar()
            .barColor(fluid)
            .proportion(amount / capacity)
            .suffix(Component.text(" ")
                .append(Component.text(amount.roundToInt()))
                .append(Component.text("/"))
                .append(UnitFormat.MILLIBUCKETS.format(capacity))
            )
    }
}