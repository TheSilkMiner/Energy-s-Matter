package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad

import mezz.jei.api.IJeiHelpers
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeWrapper
import net.minecraftforge.common.crafting.IShapedRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe

internal class JeiMadRecipeWrapper(private val helpers: IJeiHelpers, val recipe: MadRecipe) : IRecipeWrapper {
    override fun getIngredients(ingredients: IIngredients) {
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.recipeOutput)
        ingredients.setInputLists(VanillaTypes.ITEM, this.helpers.stackHelper.expandRecipeItemStackInputs(this.recipe.ingredients))
    }

    fun asShaped() = this.recipe as? IShapedRecipe
}
