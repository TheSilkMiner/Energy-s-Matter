package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad

import mezz.jei.api.IJeiHelpers
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeWrapper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.common.crafting.IShapedRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe

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

    companion object {
        private fun IRecipe.wrap() = when (this) {
            is MadRecipe -> this
            is IShapedRecipe -> FakeShapedMadRecipe(this)
            else -> FakeShapelessMadRecipe(this)
        }
    }

    constructor(helpers: IJeiHelpers, recipe: IRecipe) : this(helpers, recipe.wrap())

    override fun getIngredients(ingredients: IIngredients) {
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.recipeOutput)
        ingredients.setInputLists(VanillaTypes.ITEM, this.helpers.stackHelper.expandRecipeItemStackInputs(this.recipe.ingredients))
    }

    fun asShaped() = this.recipe as? IShapedRecipe
}
