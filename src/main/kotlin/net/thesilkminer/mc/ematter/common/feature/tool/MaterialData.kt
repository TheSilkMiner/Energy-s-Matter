@file:JvmName("MD")

package net.thesilkminer.mc.ematter.common.feature.tool

import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.thesilkminer.mc.ematter.common.Items

internal data class MaterialData(val harvestLevel: Int, val baseDurability: Int, val efficiencyBaseModifier: Double = 1.0, val baseDamage: Double = 1.0,
                                 val enchantModifier: Int = 1, val baseAttackSpeed: Double = 1.0, val repairStacks: Sequence<() -> ItemStack>) {
    constructor(harvestLevel: Int, baseDurability: Int, efficiencyBaseModifier: Double = 1.0, baseDamage: Double = 1.0,
                enchantModifier: Int = 1, baseAttackSpeed: Double = 1.0, repairStack: () -> ItemStack) :
            this(harvestLevel, baseDurability, efficiencyBaseModifier, baseDamage, enchantModifier, baseAttackSpeed, sequenceOf(repairStack))
}

internal val woodMaterialData = MaterialData(0, 59, 2.0, 0.0, 15) { ItemStack(Blocks.PLANKS) }
internal val goldMaterialData = MaterialData(0, 32, 12.0, 0.0, 22) { ItemStack(net.minecraft.init.Items.GOLD_INGOT) }
internal val stoneMaterialData = MaterialData(1, 131, 4.0, 1.0, 5) { ItemStack(Blocks.COBBLESTONE) }
internal val ironMaterialData = MaterialData(2, 250, 6.0, 2.0, 14) { ItemStack(net.minecraft.init.Items.IRON_INGOT) }
internal val copperMaterialData = MaterialData(2, 200, 7.0, 2.0, 20) { ItemStack(Items.copperIngot()) }
internal val diamondMaterialData = MaterialData(3, 1561, 8.0, 3.0, 10) { ItemStack(net.minecraft.init.Items.DIAMOND) }
