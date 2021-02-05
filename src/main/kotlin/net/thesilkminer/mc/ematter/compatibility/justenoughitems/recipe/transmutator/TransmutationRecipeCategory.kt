package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.transmutator

import mezz.jei.api.IGuiHelper
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IDrawableAnimated
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeCategory
import net.minecraft.client.Minecraft
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.DelayTickTimer
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.MoleIngredientType
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.moles

class TransmutationRecipeCategory(private val guiHelper: IGuiHelper) : IRecipeCategory<JeiTransmutationRecipeWrapper> {
    companion object {

        const val ID = "ematter.transmutation_crafting"

        private const val LANGUAGE_KEY = "gui.ematter.jei.transmutation.category"

        internal val guiBackgroundLocation = NameSpacedString(MOD_ID, "textures/gui/jei_plugin/molecular_transmutation_background.png")
    }

    private val localizedTitle by lazy { LANGUAGE_KEY.toLocale() }
    private val guiBackground by lazy { this.guiHelper.createDrawable(guiBackgroundLocation.toResourceLocation(), 0, 0, 104, 71) as IDrawable }

    private val firstArrowOverlay by lazy {
        this.guiHelper.drawableBuilder(guiBackgroundLocation.toResourceLocation(), 104, 0, 5, 10)
            .buildAnimated(DelayTickTimer(0, 26, 13, 5), IDrawableAnimated.StartDirection.LEFT)
    }
    private val secondArrowOverlay by lazy {
        this.guiHelper.drawableBuilder(guiBackgroundLocation.toResourceLocation(), 104, 0, 5, 10)
            .buildAnimated(DelayTickTimer(13, 13, 13, 5), IDrawableAnimated.StartDirection.LEFT)
    }
    private val thirdArrowOverlay by lazy {
        this.guiHelper.drawableBuilder(guiBackgroundLocation.toResourceLocation(), 104, 0, 5, 10)
            .buildAnimated(DelayTickTimer(26, 0, 13, 5), IDrawableAnimated.StartDirection.LEFT)
    }

    override fun getUid() = ID
    override fun getModName() = MOD_NAME
    override fun getTitle() = this.localizedTitle

    override fun getBackground() = this.guiBackground

    override fun setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: JeiTransmutationRecipeWrapper, ingredients: IIngredients) {
        val stacks = recipeLayout.itemStacks
        val moles = recipeLayout.moles

        moles.init(0, true, 11, 11)
        moles.set(0, ingredients.getInputs(MoleIngredientType).first())

        stacks.init(0, false, 76, 10)
        stacks.set(0, ingredients.getOutputs(VanillaTypes.ITEM).single())
    }

    override fun drawExtras(minecraft: Minecraft) {
        this.firstArrowOverlay.draw(minecraft, 46, 14)
        this.secondArrowOverlay.draw(minecraft, 50, 14)
        this.thirdArrowOverlay.draw(minecraft, 54, 14)
    }
}
