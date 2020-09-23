package net.thesilkminer.mc.ematter.compatibility.justenoughitems

import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.ISubtypeRegistry
import mezz.jei.api.JEIPlugin
import mezz.jei.api.ingredients.IModIngredientRegistration
import mezz.jei.api.recipe.IRecipeCategoryRegistration
import mezz.jei.api.recipe.IRecipeWrapper
import mezz.jei.api.recipe.VanillaRecipeCategoryUid
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad.JeiMadRecipeWrapper
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad.MadRecipeCategory
import kotlin.reflect.KClass

@JEIPlugin
@Suppress("unused")
internal class JustEnoughItemsPlugin : IModPlugin {
    private val l = L(MOD_NAME, "JustEnoughItems Plugin")
    private lateinit var jeiRuntime: IJeiRuntime

    override fun registerCategories(registry: IRecipeCategoryRegistration) {
        l.info("Registering categories")
        registry.jeiHelpers.guiHelper.let {
            registry.addRecipeCategories(MadRecipeCategory(it))
        }
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        l.info("JEI Runtime available: storing it")
        this.jeiRuntime = jeiRuntime
    }

    override fun registerItemSubtypes(subtypeRegistry: ISubtypeRegistry) {
        l.info("Registering item subtypes")
    }

    override fun registerIngredients(registry: IModIngredientRegistration) {
        l.info("Registering custom ingredients")
    }

    override fun register(registry: IModRegistry) {
        this.registerCatalysts(registry)
        this.registerRecipes(registry)
        this.registerRecipeHandlers(registry)
        this.registerClickAreas(registry)
        this.registerTransferHandlers(registry)
    }

    private fun registerCatalysts(registry: IModRegistry) {
        l.info("Registering catalysts")
        MadTier.values().asSequence()
                .map { it.targetMeta }
                .sortedDescending()
                .map { ItemStack(Blocks.molecularAssemblerDevice(), 1, it) }
                .forEach { registry.addRecipeCatalyst(it, VanillaRecipeCategoryUid.CRAFTING, MadRecipeCategory.ID) }
    }

    private fun registerRecipes(registry: IModRegistry) {
        l.info("Registering recipes")
        ForgeRegistries.RECIPES.let {
            registry.addRecipes(it.filterIsInstance<MadRecipe>(), MadRecipeCategory.ID)
        }
    }

    private fun registerRecipeHandlers(registry: IModRegistry) {
        l.info("Registering recipe handlers")
        registry.handleRecipes(MadRecipeCategory.ID, MadRecipe::class) { JeiMadRecipeWrapper(registry.jeiHelpers, it) }
    }

    private fun registerClickAreas(registry: IModRegistry) {
        l.info("Registering click areas")
    }

    private fun registerTransferHandlers(registry: IModRegistry) {
        l.info("Registering transfer handlers")
    }

    private fun <T : Any> IModRegistry.handleRecipes(categoryId: String, recipeClass: KClass<T>, factory: (T) -> IRecipeWrapper) =
            this.handleRecipes(recipeClass.java, { factory(it) }, categoryId)
}
