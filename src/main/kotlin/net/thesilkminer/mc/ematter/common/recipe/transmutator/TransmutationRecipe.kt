package net.thesilkminer.mc.ematter.common.recipe.transmutator

import net.minecraft.item.crafting.IRecipe
import net.thesilkminer.mc.ematter.common.mole.Moles

@ExperimentalUnsignedTypes
interface TransmutationRecipe : IRecipe {

    val moles: Moles
    val power: ULong
}