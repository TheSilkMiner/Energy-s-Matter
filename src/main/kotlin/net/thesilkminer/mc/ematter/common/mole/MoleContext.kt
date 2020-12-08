package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.item.ItemStack
import kotlin.math.roundToInt

data class MoleContext(val meta: Int, val maxDurability: Int, val durability: Int = maxDurability - meta) {

    /** percentage of durability that is left; rounded to one decimal point and represented as an int (*100) */
    val durabilityPercentage: Int = ((this.durability.toDouble() / this.maxDurability.toDouble()) * 10).roundToInt() * 10
}

fun ItemStack.createMoleContext() = MoleContext(this.metadata, this.maxDamage)
