package net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole

import mezz.jei.api.ingredients.IIngredientHelper
import mezz.jei.api.recipe.IFocus
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.mole.Moles

object MoleIngredientHelper : IIngredientHelper<MoleWrapper> {

    private const val LANGUAGE_KEY = "gui.ematter.jei.ingredient.mole.name"
    private val name by lazy { LANGUAGE_KEY.toLocale() }

    override fun translateFocus(focus: IFocus<MoleWrapper>, focusFactory: IIngredientHelper.IFocusFactory): IFocus<MoleWrapper> =
        focusFactory.createFocus(focus.mode, defaultMoleWrapper)

    override fun getMatch(ingredients: MutableIterable<MoleWrapper>, ingredientToMatch: MoleWrapper): MoleWrapper? =
        if (ingredientToMatch in ingredients) ingredientToMatch else null

    override fun getDisplayName(ingredient: MoleWrapper) = this.name

    override fun getUniqueId(ingredient: MoleWrapper) =
        this.getWildcardId(ingredient) // we want all mole ingredients (no matter the count or something else) to behave the same during comparing, blacklisting and so on

    override fun getWildcardId(ingredient: MoleWrapper) =
        "${this.getModId(ingredient)}:${this.getResourceId(ingredient)}"

    override fun getModId(ingredient: MoleWrapper) = MOD_ID // since we are currently the only ones adding moles..

    override fun getResourceId(ingredient: MoleWrapper) = "mole" // no mod id is intended

    override fun copyIngredient(ingredient: MoleWrapper) = ingredient.copy()

    override fun isValidIngredient(ingredient: MoleWrapper) = ingredient.count >= 0

    override fun isIngredientOnServer(ingredient: MoleWrapper) = true

    override fun getErrorInfo(ingredient: MoleWrapper?) =
        if (ingredient == null) "null"
        else "${ingredient.count}xingredient.mole ${this.getUniqueId(ingredient)}" // similar formatting as item stacks
}
