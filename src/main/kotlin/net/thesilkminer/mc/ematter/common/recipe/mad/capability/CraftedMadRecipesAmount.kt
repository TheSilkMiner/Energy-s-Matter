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

@file:JvmName("CMRA")

package net.thesilkminer.mc.ematter.common.recipe.mad.capability

import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.crafting.IRecipe
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.INBTSerializable
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString

@CapabilityInject(CraftedMadRecipesAmount::class)
internal lateinit var craftedMadRecipesAmountCapability: Capability<CraftedMadRecipesAmount>

internal val EntityPlayer.craftedMadRecipesAmount get() = this.getCapability(craftedMadRecipesAmountCapability, null)

internal interface CraftedMadRecipesAmount : INBTSerializable<NBTTagCompound> {
    fun findAmountFor(name: NameSpacedString): Long
    fun findAmountFor(recipe: IRecipe) = this.findAmountFor(recipe.registryName!!.toNameSpacedString())
    fun increaseAmountFor(name: NameSpacedString)
    fun increaseAmountFor(recipe: IRecipe) = this.increaseAmountFor(recipe.registryName!!.toNameSpacedString())
}

internal class CraftedMadRecipesAmountCapability : CraftedMadRecipesAmount {
    private val recipeData = Object2LongAVLTreeMap<NameSpacedString>().apply { this.defaultReturnValue(0) }

    override fun findAmountFor(name: NameSpacedString) = this.recipeData.getLong(name)

    override fun increaseAmountFor(name: NameSpacedString) {
        this.recipeData[name] = this.findAmountFor(name) + 1L
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) =
            nbt?.keySet?.forEach { this.recipeData[it.toNameSpacedString()] = nbt.getLong(it).let { amount -> if (amount < 0) 0 else amount } } ?: Unit

    override fun serializeNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        this.recipeData.asSequence().filter { it.value != 0L }.forEach { (k, v) -> tag.setLong(k.toString(), v) }
        return tag
    }
}
