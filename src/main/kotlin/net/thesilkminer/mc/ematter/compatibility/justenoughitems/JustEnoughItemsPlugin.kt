package net.thesilkminer.mc.ematter.compatibility.justenoughitems

import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.ISubtypeRegistry
import mezz.jei.api.JEIPlugin
import mezz.jei.api.ingredients.IModIngredientRegistration
import mezz.jei.api.recipe.IRecipeCategoryRegistration
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME

@JEIPlugin
@Suppress("unused")
internal class JustEnoughItemsPlugin : IModPlugin {
    private val l = L(MOD_NAME, "JustEnoughItems Plugin")
    private lateinit var jeiRuntime: IJeiRuntime

    override fun registerCategories(registry: IRecipeCategoryRegistration) {
        l.info("Registering categories")
    }

    override fun register(registry: IModRegistry) {
        l.info("Performing final registration")
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
}
