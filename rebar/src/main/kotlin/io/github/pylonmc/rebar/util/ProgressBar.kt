package io.github.pylonmc.rebar.util

import io.github.pylonmc.rebar.fluid.RebarFluid
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color
import kotlin.math.roundToInt

class ProgressBar {

    var proportion: Double? = null
    var bars: Int = 20
    var filledBars: Int? = null
    var barColor: TextColor = TextColor.fromHexString("#ffffff")!!
    var emptyColor: TextColor = TextColor.fromHexString("#4d4d4d")!!
    var barString = "|"
    var leftString = ""
    var rightString = ""
    var suffix: ComponentLike? = null

    fun bars(bars: Int) = apply { this.bars = bars }

    fun proportion(proportion: Double) = apply { this.proportion = proportion }
    fun filledBars(filledBars: Int) = apply { this.filledBars = filledBars }

    fun barColor(barColor: TextColor) = apply { this.barColor = barColor }
    fun barColor(barColor: Color) = apply { this.barColor = barColor.toTextColor() }
    fun barColor(fluid: RebarFluid) = apply { this.barColor = fluid.color }

    fun emptyColor(emptyColor: TextColor) = apply { this.emptyColor = emptyColor }
    fun emptyColor(emptyColor: Color) = apply { this.emptyColor = emptyColor.toTextColor() }

    fun barString(barString: String) = apply { this.barString = barString }
    fun leftString(leftString: String) = apply { this.leftString = leftString }
    fun rightString(rightString: String) = apply { this.rightString = rightString }

    fun suffix(component: ComponentLike) = apply { this.suffix = component }

    fun build(): Component {
        var filledBars: Int? = this.filledBars
        if (proportion != null) {
            filledBars = (bars * proportion!!).roundToInt()
        }
        check(filledBars != null) { "You need to call either `proportion()` or `filledBars()`" }

        return Component.text(leftString)
            .append(Component.text(barString.repeat(filledBars)).color(barColor))
            .append(Component.text(barString.repeat(bars - filledBars)).color(emptyColor))
            .append(Component.text(rightString))
            .append(if (suffix == null) Component.empty() else Component.text(" ").append(suffix!!))
    }
}