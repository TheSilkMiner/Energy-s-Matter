package net.thesilkminer.mc.ematter.common.feature.seebeck

import net.minecraft.block.Block
import net.minecraft.init.Blocks

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
internal enum class TemperatureSources(val block: Block, val value: Int) {
    // TODO("Move to temperature tables")

    //coolants
    WATER(Blocks.WATER, 280),
    WATER_FLOW(Blocks.FLOWING_WATER, 280), //a source block is actually flowing water
    SNOW(Blocks.SNOW, 268),
    ICE(Blocks.ICE, 263),
    ICE_PACKED(Blocks.PACKED_ICE, 253),

    //heat sources
    LIT_FURNACE(Blocks.LIT_FURNACE, 500),
    FIRE(Blocks.FIRE, 800),
    LAVA(Blocks.LAVA, 1000),
    LAVA_FLOW(Blocks.FLOWING_LAVA, 1000), //same as for the water
    MAGMA(Blocks.MAGMA, 1400)

}
