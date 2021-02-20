package net.thesilkminer.mc.ematter.common.recipe.anvil

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.JsonUtils
import net.minecraft.util.NonNullList
import net.minecraft.world.World
import net.minecraftforge.common.crafting.IRecipeFactory
import net.minecraftforge.common.crafting.JsonContext
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.mod.common.recipe.RecipeLoadingProcessor
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.feature.anvil.AnvilRecipeContext
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper

internal class SmashingRecipe(private val group: NameSpacedString?, private val ingredient: Ingredient, override val amountOfHits: Byte,
                              private val output: ItemStack) : IForgeRegistryEntry.Impl<IRecipe>(), AnvilRecipe {
    override val kind: AnvilRecipe.Kind = AnvilRecipe.Kind.SMASHING
    override fun canFit(width: Int, height: Int): Boolean = width * height > 1
    override fun getRecipeOutput(): ItemStack = this.output.copy()
    override fun getGroup(): String = this.group?.toString() ?: ""
    override fun getIngredients(): NonNullList<Ingredient> = NonNullList.from(Ingredient.EMPTY, this.ingredient)
    override fun getCraftingResult(inv: InventoryCrafting): ItemStack = this.recipeOutput.copy()
    override fun getRemainingItems(inv: InventoryCrafting): NonNullList<ItemStack> = NonNullList.create()

    override fun matches(inv: InventoryCrafting, worldIn: World?): Boolean {
        if (inv !is CraftingInventoryWrapper) return false
        if (inv.containerClass != AnvilRecipeContext.Wrapper::class) return false

        val wrapped = inv.wrappedContainer
        if (wrapped !is AnvilRecipeContext.Wrapper) return false

        val context = wrapped.asContext()
        return this.ingredient.apply(context.stack) && context.hits == this.amountOfHits
    }
}

@Suppress("unused")
internal class SmashingRecipeSerializer : IRecipeFactory {
    // Not really that good, since we are reaching across modules, but this is what it is
    // This will be removed in 1.13+ and replaced with actual serializers hooked into Forge
    // Registries so...
    companion object Companion private val l = RecipeLoadingProcessor.l

    override fun parse(context: JsonContext, json: JsonObject): IRecipe {
        val group = JsonUtils.getString(json, "group", "")
        if (group.isNotEmpty() && group.indexOf(':') == -1) l.warn("Group '$group' does not have a namespace: this will not survive a 1.13+ upgrade!")

        val ingredient = RecipeLoadingProcessor.getIngredient(JsonUtils.getJsonObject(json, "ingredient"), context)
        if (ingredient == Ingredient.EMPTY) throw JsonSyntaxException("Empty ingredient is not allowed in the inputs of a smashing recipe")

        val hits = JsonUtils.getInt(json, "hits")
        if (hits <= 0) throw JsonSyntaxException("Expected to find a positive amount of hits for a smashing recipe, instead got $hits")
        if (hits > Byte.MAX_VALUE) throw JsonSyntaxException("Too many hits: maximum value is ${Byte.MAX_VALUE}, but found $hits")

        val output = RecipeLoadingProcessor.getItemStack(JsonUtils.getJsonObject(json, "result"), context, parseNbt = true)

        return SmashingRecipe(if (group.isEmpty()) null else group.toNameSpacedString(), ingredient, hits.toByte(), output)
    }
}
