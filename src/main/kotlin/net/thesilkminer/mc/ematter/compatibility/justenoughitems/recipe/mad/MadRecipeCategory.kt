package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad

import mezz.jei.api.IGuiHelper
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IGuiItemStackGroup
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeCategory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.common.Loader
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.locale.Color
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier

internal class MadRecipeCategory(private val guiHelper: IGuiHelper) : IRecipeCategory<JeiMadRecipeWrapper> {
    companion object {
        const val ID = "ematter.mad_crafting"

        private const val LANGUAGE_KEY = "gui.ematter.jei.mad.category"
        private const val FIRST_INPUT_SLOT = 1
        private const val OUTPUT_SLOT = 0

        private val guiBackgroundLocation = NameSpacedString(MOD_ID, "textures/gui/jei_plugin/molecular_assembler_device_background.png")
        private val unknownModMarker by lazy { "gui.ematter.jei.shared.unknown_mod".toLocale() }
    }

    private val localizedTitle by lazy { LANGUAGE_KEY.toLocale() }
    private val guiBackground by lazy { this.guiHelper.createDrawable(guiBackgroundLocation.toResourceLocation(), 0, 0, 104, 143) as IDrawable }
    private val categoryIcon by lazy { this.guiHelper.createDrawableIngredient(ItemStack(Blocks.molecularAssemblerDevice(), 1, MadTier.ELITE.targetMeta)) as IDrawable }

    override fun getUid() = ID
    override fun getModName() = MOD_NAME
    override fun getTitle() = this.localizedTitle
    override fun getBackground() = this.guiBackground
    override fun getIcon() = this.categoryIcon

    override fun drawExtras(minecraft: Minecraft) {
        super.drawExtras(minecraft)
    }

    override fun getTooltipStrings(mouseX: Int, mouseY: Int): MutableList<String> {
        return super.getTooltipStrings(mouseX, mouseY)
    }

    override fun setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: JeiMadRecipeWrapper, ingredients: IIngredients) {
        val slots = recipeLayout.itemStacks
        slots.initLayout()

        val inputs = ingredients.getInputs(VanillaTypes.ITEM)
        val output = ingredients.getOutputs(VanillaTypes.ITEM).single()

        val shapedRecipe = recipeWrapper.asShaped()
        if (shapedRecipe != null) {
            val width = shapedRecipe.recipeWidth
            val height = shapedRecipe.recipeHeight

            var ingredientCount = 0
            (0 until 5).forEach { row ->
                val skipRow = row.shouldSkipRowFor(height)
                (0 until 5).forEach { column ->
                    val skipColumn = skipRow || column.shouldSkipColumnFor(width)
                    val index = FIRST_INPUT_SLOT + row * 5 + column

                    if (!skipColumn) {
                        slots.set(index, inputs[ingredientCount])
                        ++ingredientCount
                    }
                }
            }
        } else {
            val count = inputs.count()

            var ingredientCount = 0
            (0 until 5).forEach { row ->
                (0 until 5).forEach { column ->
                    val index = FIRST_INPUT_SLOT + row * 5 + column

                    if (ingredientCount < count) {
                        slots.set(index, inputs[ingredientCount])
                        ++ingredientCount
                    }
                }
            }

            recipeLayout.setShapeless()
        }

        slots.set(OUTPUT_SLOT, output)

        slots.addTooltipCallback { slotIndex, _, ingredient, tooltip ->
            if (slotIndex != OUTPUT_SLOT) return@addTooltipCallback
            val recipeNamespace = recipeWrapper.recipe.registryName?.namespace ?: return@addTooltipCallback
            val outputNamespace = ingredient.item.registryName?.namespace ?: return@addTooltipCallback

            if (recipeNamespace != outputNamespace) {
                val modName = Loader.instance().modList.firstOrNull { it.modId == outputNamespace } ?: unknownModMarker
                tooltip += "gui.ematter.jei.shared.recipe_by".toLocale("${TextFormatting.DARK_BLUE}$modName", color = Color.GRAY)
            }

            if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips || GuiScreen.isShiftKeyDown()) {
                tooltip += "gui.ematter.jei.shared.recipe_name".toLocale(recipeWrapper.recipe.registryName, color = Color.DARK_GRAY)
            }
        }
    }

    private fun IGuiItemStackGroup.initLayout() {
        this.init(OUTPUT_SLOT, false, 43, 10)
        (0 until 5).forEach { row ->
            (0 until 5).forEach { column ->
                this.init(FIRST_INPUT_SLOT + column + (row * 5), true, 7 + column * 18, 46 + row * 18)
            }
        }
    }

    private fun Int.shouldSkipRowFor(height: Int) = this.shouldSkipFor(height, "height was $height, but max is 5")
    private fun Int.shouldSkipColumnFor(width: Int) = this.shouldSkipFor(width, "width was $width, but max is 5")

    private fun Int.shouldSkipFor(value: Int, exceptionMessage: String) = when (value) {
        0 -> true
        1 -> this != 2
        2 -> this != 1 && this != 2
        3 -> this == 0 || this == 4
        4 -> this == 4
        5 -> false
        else -> throw IllegalStateException("Broken recipe: $exceptionMessage")
    }
}
