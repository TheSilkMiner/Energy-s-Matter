@file:JvmName("ZA")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared

import crafttweaker.CraftTweakerAPI
import crafttweaker.IAction
import crafttweaker.api.recipes.ICraftingRecipe
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.function.Consumer
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.sequence.ZenSequence
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID

internal abstract class AddRecipeAction(protected val name: NameSpacedString, recipeSupplier: () -> IRecipe) : IAction {

    protected val recipe by lazy(recipeSupplier)

    override fun apply() {
        if (ForgeRegistries.RECIPES.getValue(this.name.toResourceLocation()) != null) {
            return CraftTweakerAPI.logError("Unable to register recipe with name '${this.name}' since it is conflicting with another recipe with the same name")
        }

        ForgeRegistries.RECIPES.register(this.recipe.setRegistryName(this.name.toResourceLocation()))
    }
}

internal abstract class RemoveRecipeAction(protected val name: NameSpacedString, recipeSupplier: () -> IRecipe?) : IAction {

    protected val recipe by lazy(recipeSupplier)

    override fun apply() {
        if (this.recipe == null) {
            return CraftTweakerAPI.logError("Unable to unregister recipe with name '${this.name}' since it does not exist")
        }

        ForgeRegistries.RECIPES.unregister(this.recipe!!)
    }

    private fun <T : IForgeRegistryEntry<T>> IForgeRegistry<T>.unregister(obj: T) =
        (this as? IForgeRegistryModifiable<T>)?.remove(obj.registryName)
            ?: CraftTweakerAPI.logError("Unable to unregister recipe with name '${this@RemoveRecipeAction.name}': operation not allowed")
}

internal abstract class DumpAction(private val recipeZenSequenceSupplier: () -> ZenSequence<out ICraftingRecipe>) : IAction {

    override fun apply() {
        this.recipeZenSequenceSupplier().forEach(object : Consumer<ICraftingRecipe> {
            override fun accept(t: ICraftingRecipe) = CraftTweakerAPI.logInfo("> ${t.toCommandString()}")
        })

        CraftTweakerAPI.logInfo("Dump completed")
    }
}

internal class ScheduledActionGroupAction<in T : IAction>(private val actions: Sequence<T>, private val info: String? = null, private val validator: (() -> Boolean)? = null) : IAction {

    override fun validate() = this.validator?.let { it() } ?: this.actions.all(IAction::validate)
    override fun describe() = "" // Disable logging, since we are going to do our own
    override fun describeInvalid() = "One or more of the given actions are not valid"

    override fun apply() {
        CraftTweakerAPI.logInfo("Applying sequentially a set of ${this.actions.count()} scheduled actions${this.info?.let { ": $it" } ?: ""}")

        this.actions.forEach {
            CraftTweakerAPI.logInfo("> ${it.describe()}")
            it.apply()
        }

        CraftTweakerAPI.logInfo("Application completed")
    }
}

internal class ScheduledActionGroupRemoveAction<in T : IAction>(private val wrapped: ScheduledActionGroupAction<T>) : RemoveRecipeAction(NameSpacedString(MOD_ID, "unregistered"), { null }) {

    override fun validate() = this.wrapped.validate()
    override fun describe() = this.wrapped.describe()
    override fun describeInvalid() = this.wrapped.describeInvalid()
    override fun apply() = this.wrapped.apply()
}
