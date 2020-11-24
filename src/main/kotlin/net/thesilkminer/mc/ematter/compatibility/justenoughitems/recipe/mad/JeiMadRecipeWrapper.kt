package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad

import mezz.jei.api.IJeiHelpers
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.common.crafting.IShapedRecipe
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.api.locale.Color
import net.thesilkminer.mc.boson.api.locale.toLocale
import net.thesilkminer.mc.boson.prefab.energy.toUserFriendlyAmount
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.craftedMadRecipesAmountCapability
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.renderArrow
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.renderLine
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.renderNormalText
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.renderSmallText
import org.lwjgl.input.Mouse
import java.lang.reflect.Field
import kotlin.math.abs
import kotlin.math.floor
import kotlin.reflect.KClass

internal class JeiMadRecipeWrapper(private val helpers: IJeiHelpers, val recipe: MadRecipe) : IRecipeWrapper {
    private open class FakeShapelessMadRecipe(private val recipe: IRecipe) : MadRecipe {
        @ExperimentalUnsignedTypes override fun getPowerRequiredFor(player: EntityPlayer) = 0UL
        override fun canFit(width: Int, height: Int) = this.recipe.canFit(width, height)
        override fun hashCode() = this.recipe.hashCode()
        override fun getRegistryType(): Class<IRecipe> = this.recipe.registryType
        override fun getGroup(): String = this.recipe.group
        override fun getRemainingItems(inv: InventoryCrafting): NonNullList<ItemStack> = this.recipe.getRemainingItems(inv)
        override fun equals(other: Any?) = this.recipe == other
        override fun toString() = this.recipe.toString()
        override fun getRecipeOutput(): ItemStack = this.recipe.recipeOutput
        override fun getRegistryName() = this.recipe.registryName
        override fun isDynamic() = this.recipe.isDynamic
        override fun getCraftingResult(inv: InventoryCrafting): ItemStack = this.recipe.getCraftingResult(inv)
        override fun getIngredients(): NonNullList<Ingredient> = this.recipe.ingredients
        override fun setRegistryName(name: ResourceLocation?): IRecipe = this.recipe.setRegistryName(name)
        override fun matches(inv: InventoryCrafting, worldIn: World) = this.recipe.matches(inv, worldIn)
    }

    private class FakeShapedMadRecipe(private val recipe: IShapedRecipe) : FakeShapelessMadRecipe(recipe), IShapedRecipe {
        override fun getRecipeHeight() = this.recipe.recipeHeight
        override fun getRecipeWidth() = this.recipe.recipeWidth
    }

    @ExperimentalUnsignedTypes
    private object FakeDoNothingSteppingFunction : SteppingFunction {
        override fun getPowerCostAt(x: Long) = 0UL
    }

    companion object {
        private val graphCoordinatesX = 104..182
        private val graphCoordinatesY = 55..127

        private val graphXAxisBegin = 108.0 to 122.0
        private val graphXAxisEnd = 174.0 to 122.0
        private val graphYAxisBegin = 108.0 to 122.0
        private val graphYAxisEnd = 108.0 to 63.0
        private const val GRAPH_AXIS_COLOR = 0x5AA4F74F
        private const val GRAPH_AXIS_TEXT_COLOR = GRAPH_AXIS_COLOR or 0x000000FF

        private val graphXMarkers = doubleArrayOf(119.0, 131.0, 143.0, 156.0, 168.0)

        private val graphCurrentValueBegin = graphXMarkers[1] to 120.0
        private val graphCurrentValueEnd = graphXMarkers[1] to 61.0
        private const val GRAPH_CURRENT_COLOR = 0xFF5D5D4D.toInt()
        private const val GRAPH_OTHER_COLOR = 0xA0A0A01D.toInt()

        private val graphCurrentPowerCoordinates = graphXAxisBegin.first to graphXAxisBegin.second + 4
        private const val GRAPH_CURRENT_TEXT_COLOR = 0x00000000

        private val formulaCoordinates = arrayOf(108.0 to 12.0, 108.0 to 24.0, 108.0 to 36.0)
        private const val FORMULA_TEXT_COLOR = 0x000000FF
        private const val FORMULA_ERROR_TEXT_COLOR = 0xFF0000FF.toInt()
        private const val FORMULA_WARNING_TEXT_COLOR = 0xFFD700FF.toInt()

        private val fieldReferences = mutableMapOf<KClass<*>, Field?>()
        private val fieldReferenceComputingFunction = { key: KClass<*> ->
            try { key.java.getDeclaredField("steppingFunction").apply { this.isAccessible = true } } catch (e: ReflectiveOperationException) { null }
        }

        private fun IRecipe.wrap() = when (this) {
            is MadRecipe -> this
            is IShapedRecipe -> FakeShapedMadRecipe(this)
            else -> FakeShapelessMadRecipe(this)
        }
    }

    constructor(helpers: IJeiHelpers, recipe: IRecipe) : this(helpers, recipe.wrap())

    private val errorIcon by lazy { this.helpers.guiHelper.createDrawable(MadRecipeCategory.guiBackgroundLocation.toResourceLocation(), 183, 0, 25, 21) as IDrawable }

    @Suppress("EXPERIMENTAL_API_USAGE") private val recipeSteppingFunction by lazy { this.recipe.findSteppingFunction() }
    @Suppress("EXPERIMENTAL_API_USAGE") private val steppingFunctionFormula by lazy { this.recipeSteppingFunction?.grabFormula() }

    override fun getIngredients(ingredients: IIngredients) {
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.recipeOutput)
        ingredients.setInputLists(VanillaTypes.ITEM, this.helpers.stackHelper.expandRecipeItemStackInputs(this.recipe.ingredients))
    }

    @ExperimentalUnsignedTypes
    override fun drawInfo(minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int) {
        super.drawInfo(minecraft, recipeWidth, recipeHeight, mouseX, mouseY)

        if (this.recipeSteppingFunction == null) return this.renderSteppingFunctionError(minecraft)

        val currentAmount = minecraft.player.getCapability(craftedMadRecipesAmountCapability, null)?.findAmountFor(this.recipe) ?: return this.renderSteppingFunctionError(minecraft)

        val steppingFunctionValues = (currentAmount until currentAmount + graphXMarkers.size)
                .map { it - 1 }
                .map { if (it < 0) null else this.recipeSteppingFunction!!.getPowerCostAt(it) }
                .toTypedArray()
        if (steppingFunctionValues.filterNotNull().isEmpty()) return this.renderSteppingFunctionError(minecraft)

        this.renderGraphElements(minecraft)
        this.renderSteppingFunction(minecraft, steppingFunctionValues)
        this.renderSteppingFunctionFormula(minecraft)
    }

    private fun renderSteppingFunctionError(minecraft: Minecraft) = this.errorIcon.draw(minecraft, 131, 80)

    private fun renderGraphElements(minecraft: Minecraft) {
        graphXMarkers.forEach {
            if (it != graphCurrentValueBegin.first) {
                renderLine(minecraft, it to graphCurrentValueBegin.second, it to graphCurrentValueEnd.second, GRAPH_OTHER_COLOR, 0.6)
            }
        }
        renderLine(minecraft, graphCurrentValueBegin, graphCurrentValueEnd, GRAPH_CURRENT_COLOR, 1.0)
        renderArrow(minecraft, graphXAxisBegin, graphXAxisEnd, GRAPH_AXIS_COLOR, 1.0)
        renderArrow(minecraft, graphYAxisBegin, graphYAxisEnd, GRAPH_AXIS_COLOR, 1.0)
        graphXMarkers.forEach {
            val markerYBegin = graphXAxisBegin.second
            val markerYEnd = markerYBegin + 3.5
            renderLine(minecraft, it to markerYBegin, it to markerYEnd, GRAPH_AXIS_COLOR, 0.8)
        }
    }

    @ExperimentalUnsignedTypes
    private fun renderSteppingFunction(minecraft: Minecraft, steppingFunctionValues: Array<ULong?>) {
        val min = steppingFunctionValues.filterNotNull().min()!! // There surely is a min, otherwise we wouldn't be rendering anything
        val max = steppingFunctionValues.filterNotNull().max()!! // There surely is a max, otherwise we wouldn't be rendering anything

        val coordinatePairs = graphXMarkers.mapIndexed { index, marker -> steppingFunctionValues[index]?.let { marker to it.toYCoordinate(min, max) } }

        (1 until coordinatePairs.count()).forEach { endIndex ->
            val beginningCoordinates = coordinatePairs[endIndex - 1]
            val endingCoordinates = coordinatePairs[endIndex]
            if (beginningCoordinates == null || endingCoordinates == null) return@forEach
            renderLine(minecraft, beginningCoordinates, endingCoordinates, GRAPH_OTHER_COLOR, 1.3)
        }

        this.renderSteppingFunctionBoundsMarkers(minecraft, min, steppingFunctionValues[1], max)
    }

    @ExperimentalUnsignedTypes
    private fun renderSteppingFunctionBoundsMarkers(minecraft: Minecraft, min: ULong, current: ULong?, max: ULong) {
        if (min == max) return this.renderSteppingFunctionBoundsMarkers(minecraft, min, current, max + 1.toULong())
        val minText = min.toUserFriendlyAmount(decimalDigits = 0).replace(" ", "")
        val maxText = max.toUserFriendlyAmount(decimalDigits = 0).replace(" ", "")
        val currentText = "gui.ematter.jei.mad.step.power.current".toLocale(current?.toUserFriendlyAmount(decimalDigits = 4) ?: "gui.ematter.jei.mad.step.power.unknown".toLocale())
        renderSmallText(minecraft, minText, graphYAxisBegin.first + 1.5 to graphCurrentValueBegin.second - 8.0, GRAPH_AXIS_TEXT_COLOR)
        renderSmallText(minecraft, maxText, graphYAxisBegin.first + 1.5 to graphCurrentValueEnd.second, GRAPH_AXIS_TEXT_COLOR)
        renderSmallText(minecraft, currentText, graphCurrentPowerCoordinates, GRAPH_CURRENT_TEXT_COLOR)
    }

    @ExperimentalUnsignedTypes
    private fun renderSteppingFunctionFormula(minecraft: Minecraft) {
        val formulaLines = this.steppingFunctionFormula
        if (formulaLines == null) {
            renderNormalText(minecraft, "gui.ematter.jei.mad.formula.unknown.top".toLocale(), formulaCoordinates[0], FORMULA_WARNING_TEXT_COLOR)
            return renderNormalText(minecraft, "gui.ematter.jei.mad.formula.unknown.middle".toLocale(), formulaCoordinates[1], FORMULA_WARNING_TEXT_COLOR)
        }

        if (formulaLines.isEmpty() || formulaLines.count() > 3) {
            renderNormalText(minecraft, "gui.ematter.jei.mad.formula.error.top".toLocale(), formulaCoordinates[0], FORMULA_ERROR_TEXT_COLOR)
            renderNormalText(minecraft, "gui.ematter.jei.mad.formula.error.middle".toLocale(), formulaCoordinates[1], FORMULA_ERROR_TEXT_COLOR)
            return renderNormalText(minecraft, "gui.ematter.jei.mad.formula.error.bottom".toLocale(), formulaCoordinates[2], FORMULA_ERROR_TEXT_COLOR)
        }
        formulaLines.forEachIndexed { index, line ->
            renderNormalText(minecraft, line, formulaCoordinates[index], FORMULA_TEXT_COLOR)
        }
    }

    @ExperimentalUnsignedTypes
    override fun getTooltipStrings(mouseX: Int, mouseY: Int): List<String> {
        if (mouseX !in graphCoordinatesX || mouseY !in graphCoordinatesY) return super.getTooltipStrings(mouseX, mouseY)
        val list = mutableListOf<String>()

        list += if (this.recipeSteppingFunction == null) {
            "gui.ematter.jei.mad.step.unknown".toLocale(color = Color.RED)
        } else {
            try {
                val exactMouseX = mouseX.adjustToDouble()
                val distances = graphXMarkers.map { abs(it - exactMouseX) }
                val nearest = distances.min()!! // I know there's at least one element
                val targetX = distances.asSequence().mapIndexed { index, value -> index to value }.first { it.second == nearest }.first
                val capability = Minecraft.getMinecraft().player.getCapability(craftedMadRecipesAmountCapability, null)!!.findAmountFor(this.recipe)
                if (capability == 0L && targetX == 0) {
                    "gui.ematter.jei.mad.step.none".toLocale(color = Color.GOLD)
                } else {
                    val targetValue = capability - 1 + targetX
                    this.recipeSteppingFunction!!.getPowerCostAt(targetValue).toUserFriendlyAmount(decimalDigits = 4)
                }
            } catch (e: Exception) {
                "gui.ematter.jei.mad.step.error".toLocale(color = Color.RED)
            }
        }

        return list
    }

    fun asShaped() = this.recipe as? IShapedRecipe

    @ExperimentalUnsignedTypes
    private fun MadRecipe.findSteppingFunction() = when (this) {
        is FakeShapelessMadRecipe -> FakeDoNothingSteppingFunction
        else -> fieldReferences.computeIfAbsent(this::class, fieldReferenceComputingFunction)?.let { it[this].uncheckedCast<SteppingFunction>() }
    }

    @ExperimentalUnsignedTypes
    private fun ULong.toYCoordinate(min: ULong, max: ULong): Double {
        // (this - min) / (max - min) = (this on yAxis) / (max on yAxis)
        val maxYAxis = abs(graphCurrentValueBegin.second - graphCurrentValueEnd.second)
        val thisYAxis = if (max.toDouble() <= 0.0) 0.0 else ((this - min).toDouble() / (max - min).toDouble()) * maxYAxis
        return graphCurrentValueBegin.second - thisYAxis
    }

    // "Stolen" from Just Enough Resources
    private fun Int.adjustToDouble(): Double {
        val minecraft = Minecraft.getMinecraft()
        val scaledResolution = ScaledResolution(minecraft)
        val width = scaledResolution.scaledWidth
        val exactMouseX = (Mouse.getX() * width).toDouble() / minecraft.displayWidth.toDouble()
        val fraction = exactMouseX - floor(exactMouseX)
        return this + fraction
    }
}
