@file:JvmName("SLR")

package net.thesilkminer.mc.ematter.common.recipe.mad

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.client.util.RecipeItemHelper
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
import net.minecraftforge.common.crafting.JsonContext
import net.minecraftforge.common.util.RecipeMatcher
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.mod.common.recipe.RecipeLoadingProcessor
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.common.recipe.mad.step.get
import net.thesilkminer.mc.ematter.common.recipe.mad.step.steppingFunctionSerializerRegistry
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper

internal class ShapelessMadRecipe @ExperimentalUnsignedTypes constructor(private val group: NameSpacedString?, private val ingredients: NonNullList<Ingredient>,
                                                                         private val result: ItemStack, private val steppingFunction: SteppingFunction)
    : IForgeRegistryEntry.Impl<IRecipe>(), MadRecipe {

    private val isSimple = this.ingredients.all(Ingredient::isSimple)

    override fun canFit(width: Int, height: Int) = this.ingredients.count() <= width * height
    override fun getRecipeOutput(): ItemStack = this.result.copy()
    override fun getGroup() = this.group?.toString() ?: ""
    override fun getIngredients() = this.ingredients
    override fun getCraftingResult(inv: InventoryCrafting): ItemStack = this.recipeOutput.copy()
    @ExperimentalUnsignedTypes override fun getPowerRequiredFor(player: EntityPlayer) = this.steppingFunction.getPowerCostFor(player, this)

    override fun getRemainingItems(inv: InventoryCrafting): NonNullList<ItemStack> {
        val list = NonNullList.withSize(inv.sizeInventory, ItemStack.EMPTY)
        (0 until list.size).forEach { list[it] = ForgeHooks.getContainerItem(inv.getStackInSlot(it)) }
        return list
    }

    override fun matches(inv: InventoryCrafting, worldIn: World): Boolean {
        if (inv !is CraftingInventoryWrapper) return false
        if (inv.containerClass != MadContainer::class) return false

        var ingredientCount = 0
        val recipeItemHelper = RecipeItemHelper()
        val items = mutableListOf<ItemStack>()

        (0 until inv.sizeInventory).asSequence()
                .map(inv::getStackInSlot)
                .filterNot(ItemStack::isEmpty)
                .onEach { ++ingredientCount }
                .forEach { if (this.isSimple) recipeItemHelper.accountStack(it, 1) else items.add(it) }

        if (ingredientCount != this.ingredients.count()) return false

        return if (this.isSimple) recipeItemHelper.canCraft(this, null) else RecipeMatcher.findMatches(items, this.ingredients) != null
    }
}

@Suppress("unused")
internal class ShapelessMadRecipeSerializer : IRecipeFactory {
    // Not really that good, since we are reaching across modules, but this is what it is
    // This will be removed in 1.13+ and replaced with actual serializers hooked into Forge
    // Registries so...
    companion object Companion private val l = RecipeLoadingProcessor.l

    @ExperimentalUnsignedTypes
    override fun parse(context: JsonContext, json: JsonObject): IRecipe? {
        val group = JsonUtils.getString(json, "group", "")
        if (group.isNotEmpty() && group.indexOf(':') == -1) l.warn("Group '$group' does not have a namespace: this will not survive a 1.13+ upgrade!")

        val ingredients = NonNullList.create<Ingredient>()
        JsonUtils.getJsonArray(json, "ingredients").asSequence()
                .map { RecipeLoadingProcessor.getIngredient(it, context) }
                .forEach { ingredients.add(it) }

        if (ingredients.isEmpty()) throw JsonSyntaxException("Empty ingredient list not allowed")

        val result = RecipeLoadingProcessor.getItemStack(JsonUtils.getJsonObject(json, "result"), context, parseNbt = true)

        val serializedSteppingFunction = JsonUtils.getJsonObject(json, "stepping_function")
        val steppingFunction = steppingFunctionSerializerRegistry[serializedSteppingFunction].read(serializedSteppingFunction)

        return ShapelessMadRecipe(if (group.isNotEmpty()) group.toNameSpacedString() else null, ingredients, result, steppingFunction)
    }
}
