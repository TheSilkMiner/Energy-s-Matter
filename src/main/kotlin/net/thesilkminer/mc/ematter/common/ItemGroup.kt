@file:JvmName("IG")

package net.thesilkminer.mc.ematter.common

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier

internal val mainItemGroup: CreativeTabs = object : CreativeTabs("$MOD_ID.main") {
    override fun createIcon() = ItemStack(ItemBlocks.molecularAssemblerDevice(), 1, MadTier.ELITE.targetMeta)
    override fun hasSearchBar() = true
    override fun getSearchbarWidth() = 73
    override fun getBackgroundImage() = ResourceLocation(MOD_ID, "textures/gui/item_group/main.png")
}
