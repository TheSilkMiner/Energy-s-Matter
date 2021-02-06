package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.transmutator

import crafttweaker.api.recipes.ICraftingRecipe
import net.thesilkminer.mc.boson.compatibility.crafttweaker.naming.ZenNameSpacedString
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationRecipe
import stanhebben.zenscript.annotations.ZenGetter

interface ZenTransmutationRecipe : ICraftingRecipe {

    @get:ZenGetter
    val recipeName: ZenNameSpacedString
    val internal: TransmutationRecipe
}
