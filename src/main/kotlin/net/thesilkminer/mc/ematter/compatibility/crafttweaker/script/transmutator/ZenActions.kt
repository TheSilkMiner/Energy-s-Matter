@file:JvmName("ZA")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.transmutator

import crafttweaker.CraftTweakerAPI
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.sequence.ZenSequence
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.recipe.transmutator.TransmutationRecipe
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.AddRecipeAction
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.DumpAction
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.RemoveRecipeAction

internal class AddTransmutationRecipeAction(name: NameSpacedString, recipeSupplier: () -> ZenTransmutationRecipeImpl) : AddRecipeAction(name, recipeSupplier) {

    override fun describe() = "Registering a Molecular Transmutator recipe with name '${this.name}' for item output '${this.recipe.recipeOutput ?: "~~UNKNOWN~~"}'"
}

internal class RemoveTargetTransmutationRecipeAction(recipe: TransmutationRecipe) :
    RemoveRecipeAction(recipe.registryName?.toNameSpacedString() ?: NameSpacedString(MOD_ID, "unregistered"), { recipe }) {

    override fun describe() = "Unregistering the Molecular Transmutator recipe with name '${this.name}' with output '${this.recipe?.recipeOutput ?: "~~UNKNOWN~~"}'"
}

internal class RemoveNamedTransmutationRecipeAction(name: NameSpacedString) : RemoveRecipeAction(name, {
    (ForgeRegistries.RECIPES.getValue(name.toResourceLocation()) as? TransmutationRecipe)
        ?: null.also { CraftTweakerAPI.logError("Will be unable to recognize recipe with name '${name}' since it is not a Molecular Transmutator recipe") }
}) {

    override fun describe() = "Unregistering the Molecular Transmutator recipe with name '${this.name}' with output '${this.recipe?.recipeOutput ?: "~~UNKNOWN~~"}'"
}

internal class TransmutatorDumpAction(recipeZenSequenceSupplier: () -> ZenSequence<ZenTransmutationRecipe>) : DumpAction(recipeZenSequenceSupplier) {

    override fun describe() = "Dumping current status of Molecular Transmutator recipes"
}
