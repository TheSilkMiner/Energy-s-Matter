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

package net.thesilkminer.mc.ematter.common.shared

import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.inventory.Container
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraftforge.items.ItemStackHandler
import kotlin.reflect.KClass

internal class CraftingInventoryWrapper(val containerClass: KClass<out Container>, val wrappedContainer: Container, private val inventory: ItemStackHandler,
                                        width: Int, height: Int) : InventoryCrafting(wrappedContainer, width, height) {
    override fun getStackInSlot(index: Int): ItemStack = if (index < 0 || index >= this.inventory.slots) ItemStack.EMPTY else this.inventory.getStackInSlot(index)
    override fun clear() = (0 until this.inventory.slots).forEach { this.inventory.setStackInSlot(it, ItemStack.EMPTY) }
    override fun removeStackFromSlot(index: Int) = this.getStackInSlot(index).also { this.setInventorySlotContents(index, ItemStack.EMPTY) }
    override fun getSizeInventory() = this.inventory.slots
    override fun isEmpty() = (0 until this.inventory.slots).map { this.inventory.getStackInSlot(it) }.all { it.isEmpty }
    override fun isItemValidForSlot(index: Int, stack: ItemStack) = this.inventory.isItemValid(index, stack)
    override fun fillStackedContents(helper: RecipeItemHelper) = (0 until this.inventory.slots).map { this.inventory.getStackInSlot(it) }.forEach { helper.accountStack(it) }

    override fun decrStackSize(index: Int, count: Int): ItemStack {
        val split = this.getStackInSlot(index).let { if (it.isEmpty || count <= 0) ItemStack.EMPTY else it.splitStack(count) }
        if (!split.isEmpty) this.wrappedContainer.onCraftMatrixChanged(this)
        return split
    }

    override fun setInventorySlotContents(index: Int, stack: ItemStack) {
        this.inventory.setStackInSlot(index, stack)
        this.wrappedContainer.onCraftMatrixChanged(this)
    }
}
