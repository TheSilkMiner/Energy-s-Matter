package net.thesilkminer.mc.ematter.common.recipe.anvil

import net.minecraft.item.crafting.IRecipe

interface AnvilRecipe : IRecipe {
    enum class Kind {
        SMASHING
    }

    val amountOfHits: Byte
    val kind: Kind
}
