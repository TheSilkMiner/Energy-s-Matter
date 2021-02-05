package net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole

import mezz.jei.api.gui.IGuiIngredientGroup
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.recipe.IIngredientType
import net.thesilkminer.mc.ematter.common.mole.Moles

data class MoleWrapper(val count: Moles)

// Used for the one mole which gets rendered in the jei overlay (this way it's possible to apply special rendering to it).
// Also used for the Focus. As long as recipes include this wrapper as input or output they will get shown no matter what state the mole ingredient the user clicked on has.
val defaultMoleWrapper = MoleWrapper(1)

object MoleIngredientType : IIngredientType<MoleWrapper> {

    override fun getIngredientClass() = MoleWrapper::class.java
}

val IRecipeLayout.moles: IGuiIngredientGroup<MoleWrapper> get() = this.getIngredientsGroup(MoleIngredientType)
