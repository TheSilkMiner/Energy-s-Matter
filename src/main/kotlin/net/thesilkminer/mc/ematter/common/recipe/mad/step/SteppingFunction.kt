package net.thesilkminer.mc.ematter.common.recipe.mad.step

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.crafting.IRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.craftedMadRecipesAmountCapability

@ExperimentalUnsignedTypes
interface SteppingFunction {
    fun getPowerCostAt(x: Long): ULong
    fun getPowerCostFor(player: EntityPlayer, recipe: IRecipe) = this.getPowerCostAt(player.getCapability(craftedMadRecipesAmountCapability, null)!!.findAmountFor(recipe))
}
