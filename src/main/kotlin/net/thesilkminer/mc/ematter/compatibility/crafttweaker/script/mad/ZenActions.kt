/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

@file:JvmName("ZA")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad

import crafttweaker.CraftTweakerAPI
import crafttweaker.IAction
import crafttweaker.api.item.IItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.function.Consumer
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.sequence.ZenSequence
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe

private enum class RecipeType {
    SHAPED,
    SHAPELESS;

    override fun toString() = if (this == SHAPED) "shaped" else "shapeless"
}

internal sealed class AddRecipeAction(private val name: NameSpacedString, private val output: IItemStack, isShaped: Boolean, recipeSupplier: () -> MadRecipe) : IAction {
    private val recipe by lazy(recipeSupplier)
    private val type = if (isShaped) RecipeType.SHAPED else RecipeType.SHAPELESS

    override fun describe() = "Registering a ${this.type} Molecular Assembler Device recipe with name '${this.name}' for item output '${this.output}'"

    override fun apply() {
        if (ForgeRegistries.RECIPES.getValue(this.name.toResourceLocation()) == null) {
            return ForgeRegistries.RECIPES.register(this.recipe.setRegistryName(this.name.toResourceLocation()))
        }
        CraftTweakerAPI.logError("Unable to register recipe with name '${this.name}' since it is conflicting with another recipe with the same name")
    }
}

internal class AddShapedRecipeAction(name: NameSpacedString, output: IItemStack, recipeSupplier: () -> MadRecipe) : AddRecipeAction(name, output, true, recipeSupplier)
internal class AddShapelessRecipeAction(name: NameSpacedString, output: IItemStack, recipeSupplier: () -> MadRecipe) : AddRecipeAction(name, output, false, recipeSupplier)

internal sealed class RemoveRecipeAction(private val name: NameSpacedString, supplier: () -> IRecipe?) : IAction {
    private val targetRecipe by lazy(supplier)
    private val targetMadRecipe by lazy { if (this.targetRecipe == null) null else this.targetRecipe as? MadRecipe? }

    override fun describe() = "Unregistering the Molecular Assembler Device recipe with name '${this.name}' with output ${this.targetMadRecipe?.recipeOutput ?: "~~UNKNOWN~~"})"

    override fun apply() {
        if (this.targetRecipe == null) return CraftTweakerAPI.logError("Unable to unregister recipe with name '${this.name}' since it does not exist")
        if (this.targetMadRecipe == null) return CraftTweakerAPI.logError("Unable to unregister recipe with name '${this.name}' since it is not a Molecular Assembler Device recipe")

        ForgeRegistries.RECIPES.unregister(this.targetMadRecipe!!) // not a reloadable lazy, so if it's non-null... it's non-null
    }

    private fun <T : IForgeRegistryEntry<T>> IForgeRegistry<T>.unregister(obj: T) {
        val modifiableRegistry = this as? IForgeRegistryModifiable<T> ?: return CraftTweakerAPI.logError("Unable to unregister recipe with name '${this@RemoveRecipeAction.name}': not allowed")
        modifiableRegistry.remove(obj.registryName!!)
    }
}

internal class RemoveTargetRecipeAction(recipe: MadRecipe) : RemoveRecipeAction(recipe.registryName?.toNameSpacedString() ?: NameSpacedString(MOD_ID, "unregistered"), { recipe })
internal class RemoveNamedRecipeAction(name: NameSpacedString) : RemoveRecipeAction(name, { ForgeRegistries.RECIPES.getValue(name.toResourceLocation()) })

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

internal class ScheduledActionGroupRemoveAction<in T : IAction>(private val wrapped: ScheduledActionGroupAction<T>) : RemoveRecipeAction(NameSpacedString(MOD_ID, "ignore"), { null }) {
    override fun describe() = this.wrapped.describe()
    override fun apply() = this.wrapped.apply()
    override fun validate() = this.wrapped.validate()
    override fun describeInvalid() = this.wrapped.describeInvalid()
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal class DumpAction(private val zenMadRecipeZenSequenceSupplier: () -> ZenSequence<ZenMadRecipe>) : IAction {
    override fun describe() = "Dumping current status of Molecular Assembler Device recipes"
    override fun apply() {
        this.zenMadRecipeZenSequenceSupplier().forEach(object : Consumer<ZenMadRecipe> {
            override fun accept(t: ZenMadRecipe) = CraftTweakerAPI.logInfo("> ${t.toCommandString()}")
        })
        CraftTweakerAPI.logInfo("Dump completed")
    }
}
