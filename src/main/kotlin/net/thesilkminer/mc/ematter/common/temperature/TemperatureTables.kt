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

    internal operator fun get(block: Block) = this.tables[block] ?: this.tables[Blocks.AIR] ?: this.defaultTable

    internal operator fun set(block: Block, table: (TemperatureContext) -> Int) {
        if (this.locked)  throw IllegalStateException("Unable to modify temperature tables after loading has completed")
        this.tables[block] = table
    }
}
