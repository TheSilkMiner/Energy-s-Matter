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

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad

import crafttweaker.CraftTweakerAPI
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.recipes.IRecipeFunction
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.modid.CRAFT_TWEAKER_2
import net.thesilkminer.mc.boson.compatibility.crafttweaker.naming.ZenNameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.sequence.ZenSequence
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step.ZenSteppingFunction
import stanhebben.zenscript.annotations.Optional
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ExperimentalUnsignedTypes
@Suppress("unused")
@ZenClass("mods.ematter.mad.MolecularAssemblerDevice")
@ZenRegister
internal object ZenMolecularAssemblerDevice {
    private val noopRecipeFunction = IRecipeFunction { output, _, _ -> output }

    private val recipesToAdd = mutableListOf<AddRecipeAction>()
    private val recipesToRemove = mutableListOf<RemoveRecipeAction>()
    private var dumpAction = null as DumpAction?

    @ZenMethod("registerShaped")
    @JvmStatic
    fun registerShaped(name: ZenNameSpacedString, group: String?, ingredients: Array<Array<IIngredient?>>, result: IItemStack, steppingFunction: ZenSteppingFunction?,
                       @Optional(valueBoolean = true) allowMirroring: Boolean, @Optional recipeFunction: IRecipeFunction?) {
        if (steppingFunction == null) return CraftTweakerAPI.logError("Unable to register recipe '$name': no valid stepping function supplier -- check the logs")
        if (group != null && group.isEmpty()) return CraftTweakerAPI.logError("Unable to register recipe '$name': empty groups are invalid, use 'null' to specify no group")
        if (ingredients.isEmpty()) return CraftTweakerAPI.logError("Unable to register recipe '$name': ingredients array is empty")
        if (ingredients.any { it.isEmpty() }) return CraftTweakerAPI.logError("Unable to register recipe '$name': ingredients array contains an empty row")
        if (ingredients.map { it.count() }.distinct().count() != 1) return CraftTweakerAPI.logError("Unable to register recipe '$name': ingredients array lengths are uneven")

        if (name.nameSpace != CRAFT_TWEAKER_2) CraftTweakerAPI.logWarning("Recipe '$name' does not specify '$CRAFT_TWEAKER_2' as its namespace: this is discouraged")
        if (group != null && !group.contains(':')) CraftTweakerAPI.logWarning("Recipe '$name' has a group without a namespace: this won't work in 1.13+")
        ingredients.count().let { if (it > 5) CraftTweakerAPI.logWarning("Recipe '$name' specifies more than 5 rows ($it): this recipe cannot be crafted") }
        ingredients.first().count().let { if (it > 5) CraftTweakerAPI.logWarning("Recipe '$name' specifies more than 5 columns ($it): this recipe cannot be crafted") }
        if (ingredients.flatten().filterNotNull().any { it.hasTransformers() }) CraftTweakerAPI.logWarning("Recipe '$name' is using the old transformer pipeline: transformers won't run")
        if (steppingFunction.isUnsafe) CraftTweakerAPI.logWarning("Recipe '$name' is being registered with an unsafe stepping function")

        val action = AddShapedRecipeAction(name.toNative(), result) {
            ZenShapedMadRecipe(
                    group = if (group != null && group.isEmpty()) null else group?.toNameSpacedString(),
                    width = ingredients.first().count(),
                    height = ingredients.count(),
                    ingredients = ingredients.copyOf(),
                    output = result,
                    allowMirroring = allowMirroring,
                    steppingFunction = steppingFunction,
                    recipeFunction = recipeFunction ?: this.noopRecipeFunction
            )
        }
        this.recipesToAdd += action
    }

    @ZenMethod("registerShapeless")
    @JvmStatic
    fun registerShapeless(name: ZenNameSpacedString, group: String?, ingredients: Array<IIngredient?>, result: IItemStack, steppingFunction: ZenSteppingFunction?,
                          @Optional recipeFunction: IRecipeFunction?) {
        if (steppingFunction == null) return CraftTweakerAPI.logError("Unable to register recipe '$name': no valid stepping function supplied -- check the logs")
        if (group != null && group.isEmpty()) return CraftTweakerAPI.logError("Unable to register recipe '$name': empty groups are invalid, use 'null' to specify no group")
        if (ingredients.isEmpty()) return CraftTweakerAPI.logError("Unable to register recipe '$name': ingredients array is empty")
        if (ingredients.any { it == null }) return CraftTweakerAPI.logError("Unable to register recipe '$name': shapeless recipes don't support 'null' ingredients")

        if (name.nameSpace != CRAFT_TWEAKER_2) CraftTweakerAPI.logWarning("Recipe '$name' does not specify '$CRAFT_TWEAKER_2' as its namespace: this is discouraged")
        if (group != null && !group.contains(':')) CraftTweakerAPI.logWarning("Recipe '$name' has a group without a namespace: this won't work in 1.13+")
        ingredients.filterNotNull().count().let { if (it > 5 * 5) CraftTweakerAPI.logWarning("Recipe '$name' specified more than ${5 * 5} ingredients ($it): this recipe cannot be crafted") }
        if (ingredients.filterNotNull().any { it.hasTransformers() }) CraftTweakerAPI.logWarning("Recipe '$name' is using the old transformer pipeline: transformers won't run")
        if (steppingFunction.isUnsafe) CraftTweakerAPI.logWarning("Recipe '$name' is being registered with an unsafe stepping function")

        val action = AddShapelessRecipeAction(name.toNative(), result) {
            ZenShapelessMadRecipe(
                    group = if (group != null && group.isEmpty()) null else group?.toNameSpacedString(),
                    size = ingredients.filterNotNull().count(),
                    ingredients = ingredients.filterNotNull().toTypedArray().copyOf(),
                    output = result,
                    steppingFunction = steppingFunction,
                    recipeFunction = recipeFunction ?: this.noopRecipeFunction
            )
        }
        this.recipesToAdd += action
    }

    @ZenMethod("unregister")
    @JvmStatic
    fun unregister(recipe: ZenMadRecipe) {
        val action = RemoveTargetRecipeAction(
                recipe = recipe.internal
        )
        this.recipesToRemove += action
    }

    @ZenMethod("unregisterByName")
    @JvmStatic
    fun unregisterByName(name: ZenNameSpacedString) {
        val action = RemoveNamedRecipeAction(
                name = name.toNative()
        )
        this.recipesToRemove += action
    }

    @ZenMethod("unregisterAll")
    @JvmStatic
    fun unregisterAll() {
        val actionSequence = ForgeRegistries.RECIPES.entries.asSequence()
                .map { it.value }
                .filter { it is MadRecipe }
                .map { it as MadRecipe }
                .map { RemoveTargetRecipeAction(it) }
        val action = ScheduledActionGroupRemoveAction(
                wrapped = ScheduledActionGroupAction(
                        actions = actionSequence,
                        info = "Unregistering all Molecular Assembler Device recipes",
                        validator = { true }
                )
        )
        this.recipesToRemove += action
    }

    @ZenMethod("findAll")
    @JvmStatic
    fun findAll(): ZenSequence<ZenMadRecipe> {
        return ZenSequence(ForgeRegistries.RECIPES.asSequence().filter { it is MadRecipe }.map { if (it is ZenMadRecipe) it else (it as MadRecipe).toZen() })
    }

    @ZenMethod("dump")
    @JvmStatic
    fun dump() {
        CraftTweakerAPI.apply(DumpAction(this::findAll))
    }

    @ZenMethod("scheduleDump")
    @JvmStatic
    fun scheduleDump() {
        if (this.dumpAction != null) CraftTweakerAPI.logError("Recipe dump has already been scheduled")
        this.dumpAction = DumpAction(this::findAll)
    }

    internal fun apply() {
        CraftTweakerAPI.logInfo("Applying Molecular Assembler Device actions - Step 1: Removals")
        this.recipesToRemove.forEach(CraftTweakerAPI::apply)
        CraftTweakerAPI.logInfo("Applying Molecular Assembler Device actions - Step 2: Additions")
        this.recipesToAdd.forEach(CraftTweakerAPI::apply)
        if (this.dumpAction != null) {
            CraftTweakerAPI.apply(this.dumpAction)
        }
        CraftTweakerAPI.logInfo("Applying Molecular Assembler Device actions - Completed")
    }

    private fun ZenNameSpacedString.toNative() = NameSpacedString(this.nameSpace, this.path)
    private fun MadRecipe.toZen(): ZenMadRecipe = ZenMadRecipeWrapper(this)
}
