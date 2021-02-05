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
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.ShapedRecipes
import net.minecraft.item.crafting.ShapelessRecipes
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.client.feature.mad.MadGui
import net.thesilkminer.mc.ematter.common.Blocks
import net.thesilkminer.mc.ematter.common.feature.mad.MadContainer
import net.thesilkminer.mc.ematter.common.feature.mad.MadTier
import net.thesilkminer.mc.ematter.common.recipe.mad.MadRecipe
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole.*
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad.JeiMadRecipeWrapper
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad.MadRecipeCategory
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.transmutator.JeiTransmutationRecipeWrapper
import net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.transmutator.TransmutationRecipeCategory
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
        registry.register(MoleIngredientType, mutableSetOf(defaultMoleWrapper), MoleIngredientHelper, MoleIngredientRenderer)
    }

    override fun register(registry: IModRegistry) {
        this.registerCatalysts(registry)
        this.registerRecipes(registry)
        this.registerRecipeHandlers(registry)
        this.registerClickAreas(registry)
        this.registerTransferHandlers(registry.recipeTransferRegistry)
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
            registry.addRecipes(it.filterIsInstance<ShapedOreRecipe>(), MadRecipeCategory.ID)
            registry.addRecipes(it.filterIsInstance<ShapedRecipes>(), MadRecipeCategory.ID)
            registry.addRecipes(it.filterIsInstance<ShapelessOreRecipe>(), MadRecipeCategory.ID)
            registry.addRecipes(it.filterIsInstance<ShapelessRecipes>(), MadRecipeCategory.ID)
        }
    }

    private fun registerRecipeHandlers(registry: IModRegistry) {
        l.info("Registering recipe handlers")
        registry.handleRecipes(MadRecipeCategory.ID, MadRecipe::class) { JeiMadRecipeWrapper(registry.jeiHelpers, it) }
        registry.handleRecipes(MadRecipeCategory.ID, ShapedOreRecipe::class) { JeiMadRecipeWrapper(registry.jeiHelpers, it) }
        registry.handleRecipes(MadRecipeCategory.ID, ShapedRecipes::class) { JeiMadRecipeWrapper(registry.jeiHelpers, it) }
        registry.handleRecipes(MadRecipeCategory.ID, ShapelessOreRecipe::class) { JeiMadRecipeWrapper(registry.jeiHelpers, it) }
        registry.handleRecipes(MadRecipeCategory.ID, ShapelessRecipes::class) { JeiMadRecipeWrapper(registry.jeiHelpers, it) }
    }

    private fun registerClickAreas(registry: IModRegistry) {
        l.info("Registering click areas")
        registry.addRecipeClickArea(MadGui::class, 86 to 38, 10 to 5, MadRecipeCategory.ID, VanillaRecipeCategoryUid.CRAFTING)
    }

    private fun registerTransferHandlers(registry: IRecipeTransferRegistry) {
        l.info("Registering transfer handlers")
        registry.addRecipeTransferHandler(MadContainer::class, MadRecipeCategory.ID, 3 to 27, 28 to 54)
        registry.addRecipeTransferHandler(MadContainer::class, VanillaRecipeCategoryUid.CRAFTING, 3 to 27, 28 to 54)
    }

    private fun <T : Any> IModRegistry.handleRecipes(categoryId: String, recipeClass: KClass<T>, factory: (T) -> IRecipeWrapper) =
            this.handleRecipes(recipeClass.java, { factory(it) }, categoryId)

    private fun <T : GuiContainer> IModRegistry.addRecipeClickArea(screenClass: KClass<T>, position: Pair<Int, Int>, size: Pair<Int, Int>, vararg categoryIds: String) =
            this.addRecipeClickArea(screenClass.java, position.first, position.second, size.first, size.second, *categoryIds)

    private fun <T : Container> IRecipeTransferRegistry.addRecipeTransferHandler(containerClass: KClass<T>, categoryId: String,
                                                                                 recipeSlots: Pair<Int, Int>, inventorySlots: Pair<Int, Int>) =
            this.addRecipeTransferHandler(containerClass.java, categoryId, recipeSlots.first,
                    recipeSlots.second - recipeSlots.first, inventorySlots.first, inventorySlots.second - inventorySlots.first)
}
