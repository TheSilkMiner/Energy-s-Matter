package net.thesilkminer.mc.ematter.common.shared

import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.inventory.Container
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraftforge.items.ItemStackHandler
import kotlin.reflect.KClass

internal class CraftingInventoryWrapper(val containerClass: KClass<out Container>, private val eventHandler: Container, private val inventory: ItemStackHandler,
                                        width: Int, height: Int) : InventoryCrafting(eventHandler, width, height) {
    override fun getStackInSlot(index: Int): ItemStack = if (index < 0 || index >= this.inventory.slots) ItemStack.EMPTY else this.inventory.getStackInSlot(index)
    override fun clear() = (0 until this.inventory.slots).forEach { this.inventory.setStackInSlot(it, ItemStack.EMPTY) }
    override fun removeStackFromSlot(index: Int) = this.getStackInSlot(index).also { this.setInventorySlotContents(index, ItemStack.EMPTY) }
    override fun getSizeInventory() = this.inventory.slots
    override fun isEmpty() = (0 until this.inventory.slots).map { this.inventory.getStackInSlot(it) }.all { it.isEmpty }
    override fun isItemValidForSlot(index: Int, stack: ItemStack) = this.inventory.isItemValid(index, stack)
    override fun fillStackedContents(helper: RecipeItemHelper) = (0 until this.inventory.slots).map { this.inventory.getStackInSlot(it) }.forEach { helper.accountStack(it) }

    override fun decrStackSize(index: Int, count: Int): ItemStack {
        val split = this.getStackInSlot(index).let { if (it.isEmpty || count <= 0) ItemStack.EMPTY else it.splitStack(count) }
        if (!split.isEmpty) this.eventHandler.onCraftMatrixChanged(this)
        return split
    }

    override fun setInventorySlotContents(index: Int, stack: ItemStack) {
        this.inventory.setStackInSlot(index, stack)
        this.eventHandler.onCraftMatrixChanged(this)
    }
}
