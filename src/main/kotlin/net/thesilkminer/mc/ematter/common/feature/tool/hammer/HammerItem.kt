package net.thesilkminer.mc.ematter.common.feature.tool.hammer

import net.thesilkminer.mc.ematter.common.feature.tool.HAMMER
import net.thesilkminer.mc.ematter.common.feature.tool.MaterialData
import net.thesilkminer.mc.ematter.common.feature.tool.ToolItem

// TODO("Add behavior to the item")
internal class HammerItem(materialData: MaterialData, attackSpeedModifier: Double = -4.2, durabilityModifier: Int = 0)
    : ToolItem(HAMMER, materialData, attackSpeedModifier = attackSpeedModifier, durabilityModifier = durabilityModifier) {
    override fun isFull3D(): Boolean = true
}
