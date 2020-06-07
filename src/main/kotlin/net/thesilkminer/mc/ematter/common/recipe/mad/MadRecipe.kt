package net.thesilkminer.mc.ematter.common.recipe.mad

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.crafting.IRecipe

interface MadRecipe : IRecipe {
    @ExperimentalUnsignedTypes
    fun getPowerRequiredFor(player: EntityPlayer): ULong
}
