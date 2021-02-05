package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.transmutator

import mezz.jei.api.IJeiHelpers
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeWrapper
import net.minecraft.client.Minecraft
import net.thesilkminer.mc.boson.prefab.energy.toUserFriendlyAmount
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationRecipe
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.MoleIngredientType
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.MoleWrapper
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.defaultMoleWrapper
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.toUserFriendlyAmount
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.renderSmallText
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.smallFontRenderer

class JeiTransmutationRecipeWrapper(private val helpers: IJeiHelpers, val recipe: TransmutationRecipe) : IRecipeWrapper {

    companion object {

        private const val REQUIREMENTS_CENTER_X = 52.0

        private const val REQUIREMENTS_AMOUNT_Y = 40.0
        private const val REQUIREMENTS_POWER_Y = 51.0

        private const val REQUIREMENTS_FONT_COLOR = 0x5AA4F74F
    }

    override fun getIngredients(ingredients: IIngredients) {
        // two inputs; one is the actual input which can be used to determine how many moles are needed for this transmutation
        // and the other one allows to lookup the usages of moles in jei and see all transmutation recipes (no matter how many moles are needed)
        ingredients.setInputs(MoleIngredientType, listOf(MoleWrapper(this.recipe.moles), defaultMoleWrapper))
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.recipeOutput)
    }

    override fun drawInfo(minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int) {
        val amount = this.recipe.moles.toUserFriendlyAmount(1)
        val power = this.recipe.power.toUserFriendlyAmount(1)

        val amountX = REQUIREMENTS_CENTER_X - smallFontRenderer.getStringWidth(amount) / 2.0

        val powerX = REQUIREMENTS_CENTER_X - smallFontRenderer.getStringWidth(power) / 2.0

        renderSmallText(minecraft, amount, amountX to REQUIREMENTS_AMOUNT_Y, REQUIREMENTS_FONT_COLOR)
        renderSmallText(minecraft, power, powerX to REQUIREMENTS_POWER_Y, REQUIREMENTS_FONT_COLOR)
    }
}
