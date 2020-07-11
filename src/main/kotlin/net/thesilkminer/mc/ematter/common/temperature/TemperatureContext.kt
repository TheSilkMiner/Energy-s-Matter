package net.thesilkminer.mc.ematter.common.temperature

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

data class TemperatureContext(val world: World, val pos: BlockPos, val dayMoment: DayMoment) {
    // TODO("Check whether placing this here makes sense")
    enum class DayMoment(private val begin: Int, private val end: Int) {
        DAWN(23_000, 24_000),
        DAY(0, 12_000),
        DUSK(12_000, 13_000),
        NIGHT(13_000, 23_000);

        companion object {
            private val values = values()
            internal operator fun get(time: Int) = this.values.first { it.begin <= time && time < it.end }
            internal operator fun get(time: Long) = this[time.toInt()]
        }
    }
}
