package net.thesilkminer.mc.ematter.common.mole

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.items.IItemHandler

class MoleHandler : IItemHandler, INBTSerializable<NBTTagCompound> {

    var moles = 0

    override fun getSlots() = 1

    override fun getStackInSlot(slot: Int): ItemStack = ItemStack.EMPTY

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
        if (this.isItemValid(slot, stack)) {
            if (!simulate) moles += stack.moles
            ItemStack.EMPTY
        } else stack

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = ItemStack.EMPTY

    override fun getSlotLimit(slot: Int) = Int.MAX_VALUE

    override fun isItemValid(slot: Int, stack: ItemStack) = stack.moles > 0

    override fun serializeNBT() = NBTTagCompound().apply {
        this.setInteger("moles", moles)
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        this.moles = nbt.getInteger("moles")
    }
}
