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
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.sequence.ZenSequence
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.AddRecipeAction
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.DumpAction
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared.RemoveRecipeAction

@ExperimentalUnsignedTypes
internal class AddShapedMadRecipeAction(name: NameSpacedString, recipeSupplier: () -> ZenShapedMadRecipe) : AddRecipeAction(name, recipeSupplier) {

    override fun describe() = "Registering a shaped Molecular Assembler Device recipe with name '${this.name}' for item output '${this.recipe.recipeOutput ?: "~~UNKNOWN~~"}'"
}

@ExperimentalUnsignedTypes
internal class AddShapelessMadRecipeAction(name: NameSpacedString, recipeSupplier: () -> ZenShapelessMadRecipe) : AddRecipeAction(name, recipeSupplier) {

    override fun describe() = "Registering a shapeless Molecular Assembler Device recipe with name '${this.name}' for item output '${this.recipe.recipeOutput ?: "~~UNKNOWN~~"}'"
}

internal class RemoveTargetMadRecipeAction(recipe: MadRecipe) : RemoveRecipeAction(recipe.registryName?.toNameSpacedString() ?: NameSpacedString(MOD_ID, "unregistered"), { recipe }) {

    override fun describe() = "Unregistering the Molecular Assembler Device recipe with name '${this.name}' with output '${this.recipe?.recipeOutput ?: "~~UNKNOWN~~"}'"
}

internal class RemoveNamedMadRecipeAction(name: NameSpacedString) : RemoveRecipeAction(name, {
    (ForgeRegistries.RECIPES.getValue(name.toResourceLocation()) as? MadRecipe)
        ?: null.also { CraftTweakerAPI.logError("Will be unable to recognize recipe with name '${name}' since it is not a Molecular Assembler Device recipe") }
}) {

    override fun describe() = "Unregistering the Molecular Assembler Device recipe with name '${this.name}' with output '${this.recipe?.recipeOutput ?: "~~UNKNOWN~~"}'"
}

@ExperimentalUnsignedTypes
internal class MadDumpAction(recipeZenSequenceSupplier: () -> ZenSequence<ZenMadRecipe>) : DumpAction(recipeZenSequenceSupplier) {

    override fun describe() = "Dumping current status of Molecular Assembler Device recipes"
}
