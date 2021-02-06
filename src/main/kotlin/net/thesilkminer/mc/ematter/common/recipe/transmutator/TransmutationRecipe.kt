package net.thesilkminer.mc.ematter.common.recipe.transmutator

import net.minecraft.item.crafting.IRecipe
import net.thesilkminer.mc.ematter.common.mole.Moles

interface TransmutationRecipe : IRecipe {

    val moles: Moles
    @ExperimentalUnsignedTypes val power: ULong
}
