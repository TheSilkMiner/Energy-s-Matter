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

@file:JvmName("CIWE")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.shared

import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.player.IPlayer
import crafttweaker.api.recipes.ICraftingInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.SlotCrafting
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.ematter.common.shared.CraftingInventoryWrapper
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.toNativeStack
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.toZen

private val eventHandlerField by lazy { ObfuscationReflectionHelper.findField(Class.forName("net.minecraft.inventory.InventoryCrafting"), "field_70465_c") }
private val playerField by lazy { ObfuscationReflectionHelper.findField(Class.forName("net.minecraft.inventory.SlotCrafting"), "field_75238_b") }

internal fun CraftingInventoryWrapper.buildMarks(rowBasis: Int, columnBasis: Int, ingredients: Array<Array<IIngredient?>>): Map<String, IItemStack?> {
    val map = mutableMapOf<String, IItemStack?>()

    ingredients.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { columnIndex, ingredient ->
            ingredient?.mark?.let { map[it] = this.getStackInRowAndColumn(columnIndex + columnBasis, rowIndex + rowBasis).toZen() }
        }
    }

    return map.toMap()
}

internal fun CraftingInventoryWrapper.buildMarks(ingredients: Array<IIngredient>): Map<String, IItemStack?> {
    val map = mutableMapOf<String, IItemStack?>()
    val size = this.sizeInventory
    val visitedMarkers = BooleanArray(size)

    ingredients.forEach { ingredient ->
        ingredient.mark?.let { mark ->
            var found = false
            (0 until size).forEach { slot ->
                if (!found) {
                    val stack = this.getStackInSlot(slot)
                    if (!stack.isEmpty && !visitedMarkers[slot]) {
                        val zenStack = stack.toZen()
                        if (ingredient.matches(zenStack)) {
                            visitedMarkers[slot] = true
                            map[mark] = zenStack
                            found = true
                        }
                    }
                }
            }
        }
    }

    return map.toMap()
}

internal fun CraftingInventoryWrapper.toZen(): ICraftingInventory = ZenCraftingInventoryWrapper(this)

private val CraftingInventoryWrapper.eventHandler get() = eventHandlerField[this].uncheckedCast<Container>()
private val SlotCrafting.player get() = playerField[this].uncheckedCast<EntityPlayer>()

private class ZenCraftingInventoryWrapper(private val wrapped: CraftingInventoryWrapper) : ICraftingInventory {
    private val targetPlayer by lazy { this.wrapped.eventHandler.inventorySlots.find { it is SlotCrafting }?.let { (it as SlotCrafting).player }?.toZen() }

    override fun getPlayer(): IPlayer? = this.targetPlayer
    override fun setStack(row: Int, column: Int, stack: IItemStack?) = this.setStack(row * this.width + column, stack)
    override fun setStack(i: Int, stack: IItemStack?) = this.wrapped.setInventorySlotContents(i, stack.toNativeStack())
    override fun getHeight() = this.wrapped.height
    override fun getSize() = this.wrapped.sizeInventory
    override fun getStack(i: Int) = this.wrapped.getStackInSlot(i).toZen()
    override fun getStack(row: Int, column: Int) = this.getStack(row * this.width + column)
    override fun getInternal() = this.wrapped
    override fun getWidth() = this.wrapped.width
    override fun getItemArray(): Array<IItemStack?> = Array(this.size) { this.getStack(it) }
    override fun getStackCount() = (0 until this.size).asSequence().map(this::getStack).filterNotNull().filterNot { it.toNativeStack().isEmpty }.count()

    override fun getItems(): Array<Array<IItemStack?>> {
        val output = Array(this.height) { arrayOfNulls<IItemStack?>(this.width) }
        val list = this.itemArray
        (0 until this.height).forEach { row ->
            (0 until this.width).forEach { column ->
                output[row][column] = list[row * this.width + column]
            }
        }
        return output
    }
}
