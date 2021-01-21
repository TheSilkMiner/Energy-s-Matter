package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.item.ItemStack
import kotlin.math.roundToInt

data class MoleContext(val meta: Int, val maxDurability: Int, val durability: Int = maxDurability - meta)

fun ItemStack.createMoleContext() = MoleContext(this.metadata, this.maxDamage)

val ItemStack.moles get() = MoleTables[this.item](this.createMoleContext())
