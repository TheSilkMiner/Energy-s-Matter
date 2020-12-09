/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

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
            internal operator fun get(time: Int) = (time % DAWN.end).let { modTime -> this.values.first { it.begin <= modTime && modTime < it.end } }
            internal operator fun get(time: Long) = this[time.toInt()]
        }
    }
}

fun World.createTemperatureContext(pos: BlockPos) = TemperatureContext(this, pos, TemperatureContext.DayMoment[this.worldTime])
