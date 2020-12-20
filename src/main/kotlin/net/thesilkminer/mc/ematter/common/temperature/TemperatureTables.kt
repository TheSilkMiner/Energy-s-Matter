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

import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME

internal typealias Kelvin = Int

/*
 * Format writeup:
 *
 * {
 *   "entries": [
 *     {
 *       "conditions": [
 *         {
 *           "type": "something",
 *           "data": []
 *         }
 *       ],
 *       "temperature": 800
 *     },
 *     {
 *       "temperature": 100
 *     }
 *   ]
 * }
 */

internal object TemperatureTables {
    private val l = L(MOD_NAME, "Temperature Tables")
    private val tables = mutableMapOf<Block, (TemperatureContext) -> Kelvin>()

    @Suppress("RedundantLambdaArrow") // Needed because type inference breaks otherwise
    private val defaultTable by lazy {
        l.bigError("""
            Unable to find default 'minecraft:air' temperature table!
            This a serious error and it cannot ever happen! Replacing with fixed value, but this WILL cause issues later on
            DO NOT rely on this behavior: the mod is broken or (more probably) you are using a broken datapack!
        """.trimIndent())
        return@lazy { _: TemperatureContext -> 295 } // 295 K is room temperature
    }

    private var locked = false

    internal fun freeze() {
        this.locked = true
    }

    internal operator fun get(block: Block) = this.tables[block] ?: this.warnOnMissing(block)

    internal operator fun set(block: Block, table: (TemperatureContext) -> Kelvin) {
        if (this.locked)  throw IllegalStateException("Unable to modify temperature tables after loading has completed")
        this.tables[block] = table
    }

    private fun warnOnMissing(block: Block): (TemperatureContext) -> Kelvin {
        l.warn("The temperature table for '${block}' is currently missing! It will be replaced with air")
        this.tables[block] = this.tables[Blocks.AIR] ?: this.defaultTable
        return this.tables.getValue(block)
    }
}
