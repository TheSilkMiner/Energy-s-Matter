package net.thesilkminer.mc.ematter.compatibility.justenoughitems

import mezz.jei.api.gui.ITickTimer
import mezz.jei.gui.TickTimer

class DelayTickTimer(
    private val beginDelayTicks: Int,
    private val endDelayTicks: Int,
    private val animationDurationTicks: Int,
    private val maxValue: Int
) : ITickTimer {

    private val internal: TickTimer

    init {
        val totalTicks = this.beginDelayTicks + this.animationDurationTicks + this.endDelayTicks
        this.internal = TickTimer(totalTicks, totalTicks, false)
    }

    override fun getValue(): Int {
        val value = this.internal.value

        return when {
            value < this.beginDelayTicks -> {
                this.maxValue
            }
            value >= this.beginDelayTicks + this.animationDurationTicks -> {
                0
            }
            else -> {
                this.maxValue - Math.floorDiv(value - this.beginDelayTicks, this.animationDurationTicks / this.maxValue) - 1
            }
        }
    }

    override fun getMaxValue(): Int = this.maxValue
}
