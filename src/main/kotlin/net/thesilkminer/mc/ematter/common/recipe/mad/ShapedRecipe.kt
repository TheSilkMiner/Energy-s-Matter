@file:JvmName("SR")

package net.thesilkminer.mc.ematter.common.recipe.mad

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.JsonUtils
import net.minecraft.util.NonNullList
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.crafting.IRecipeFactory
import net.minecraftforge.common.crafting.IShapedRecipe
import net.minecraftforge.common.crafting.JsonContext
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.mod.common.recipe.RecipeLoadingProcessor
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.common.recipe.mad.step.get
import net.thesilkminer.mc.ematter.common.recipe.mad.step.steppingFunctionSerializerRegistry
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper

internal class ShapedMadRecipe @ExperimentalUnsignedTypes constructor(private val group: NameSpacedString?, private val width: Int, private val height: Int,
                                                                      private val ingredients: NonNullList<Ingredient>, private val output: ItemStack,
                                                                      private val allowMirroring: Boolean, private val steppingFunction: SteppingFunction)
    : IForgeRegistryEntry.Impl<IRecipe>(), MadRecipe, IShapedRecipe {

    override fun canFit(width: Int, height: Int) = this.width <= width && this.height <= height
    override fun getRecipeOutput(): ItemStack = this.output.copy()
    override fun getGroup() = this.group?.toString() ?: ""
    override fun getIngredients() = this.ingredients
    override fun getCraftingResult(inv: InventoryCrafting): ItemStack = this.recipeOutput.copy()
    @ExperimentalUnsignedTypes override fun getPowerRequiredFor(player: EntityPlayer) = this.steppingFunction.getPowerCostFor(player, this)
    override fun getRecipeHeight() = this.height
    override fun getRecipeWidth() = this.width

    override fun getRemainingItems(inv: InventoryCrafting): NonNullList<ItemStack> {
        val list = NonNullList.withSize(inv.sizeInventory, ItemStack.EMPTY)
        (0 until list.size).forEach { list[it] = ForgeHooks.getContainerItem(inv.getStackInSlot(it)) }
        return list
    }

    override fun matches(inv: InventoryCrafting, worldIn: World?): Boolean {
        if (inv !is CraftingInventoryWrapper) return false
        if (inv.containerClass != MadContainer::class) return false

        (0..(inv.width - this.width)).forEach { xTranslation ->
            (0..(inv.height - this.height)).forEach { yTranslation ->
                if (this.matches(inv, xTranslation, yTranslation, false)) return true
                if (this.allowMirroring && this.matches(inv, xTranslation, yTranslation, true)) return true
            }
        }

        return false
    }

    private fun matches(inv: CraftingInventoryWrapper, xZero: Int, yZero: Int, mirror: Boolean): Boolean {
        (0 until inv.width).forEach { xBasis ->
            (0 until inv.height).forEach { yBasis ->
                val x = xBasis - xZero
                val y = yBasis - yZero

                val ingredient = if (x in 0 until this.width && y in 0 until this.height) {
                    if (mirror) this.ingredients[this.width - x - 1 + y * this.width] else this.ingredients[x + y * this.width]
                } else {
                    Ingredient.EMPTY
                }

                if (!ingredient.apply(inv.getStackInRowAndColumn(xBasis, yBasis))) return false
            }
        }
        return true
    }
}

@Suppress("unused")
internal class ShapedMadRecipeSerializer : IRecipeFactory {
    // Not really that good, since we are reaching across modules, but this is what it is
    // This will be removed in 1.13+ and replaced with actual serializers hooked into Forge
    // Registries so...
    companion object Companion private val l = RecipeLoadingProcessor.l

    @ExperimentalUnsignedTypes
    override fun parse(context: JsonContext, json: JsonObject): IRecipe? {
        val group = JsonUtils.getString(json, "group", "")
        if (group.isNotEmpty() && group.indexOf(':') == -1) l.warn("Group '$group' does not have a namespace: this will not survive a 1.13+ upgrade!")

        val ingredientsMap = mutableMapOf<Char, Ingredient>()
        JsonUtils.getJsonObject(json, "key").entrySet().forEach {
            if (it.key.length != 1) throw JsonSyntaxException("Invalid key '${it.key}': not a single character")
            if (it.key.isBlank()) throw JsonSyntaxException("Invalid key '${it.key}': the symbol is reserved")
            ingredientsMap[it.key.toCharArray()[0]] = RecipeLoadingProcessor.getIngredient(it.value, context)
        }
        ingredientsMap[' '] = Ingredient.EMPTY

        val pattern = JsonUtils.getJsonArray(json, "pattern")
        if (pattern.count() == 0) throw JsonSyntaxException("Empty pattern not allowed")

        val stringPattern = Array(pattern.count()) { "" }
        (0 until stringPattern.count()).forEach {
            val line = JsonUtils.getString(pattern[it], "pattern[$it]")

            if (it > 0 && stringPattern[0].count() != line.count()) {
                throw JsonSyntaxException("Invalid pattern: line '$it' doesn't have the same amount of characters (expected ${stringPattern[0].count()}, found ${line.count()})")
            }

            stringPattern[it] = line
        }

        val width = stringPattern[0].count()
        val height = stringPattern.count()
        val ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY)
        val allowMirroring = JsonUtils.getBoolean(json, "allow_mirroring", true)

        val keys = mutableSetOf(*ingredientsMap.keys.toTypedArray())
        keys.remove(' ')

        var x = 0
        stringPattern.asSequence()
                .flatMap { it.toCharArray().asSequence() }
                .forEach {
                    val ingredient = ingredientsMap[it] ?: throw JsonSyntaxException("Undefined reference '$it' in pattern")
                    ingredients[x] = ingredient
                    keys.remove(it)
                    ++x
                }

        if (keys.isNotEmpty()) throw JsonSyntaxException("Symbols '$keys' are unused and should be removed")

        val result = RecipeLoadingProcessor.getItemStack(JsonUtils.getJsonObject(json, "result"), context, parseNbt = true)

        val serializedSteppingFunction = JsonUtils.getJsonObject(json, "stepping_function")
        val steppingFunction = steppingFunctionSerializerRegistry[serializedSteppingFunction].read(serializedSteppingFunction)

        return ShapedMadRecipe(if (group.isEmpty()) null else group.toNameSpacedString(), width, height, ingredients, result, allowMirroring, steppingFunction)
    }
}
