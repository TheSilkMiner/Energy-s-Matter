@file:JvmName("CE")

package net.thesilkminer.mc.ematter.common

import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

internal fun bindPlayerInventory(playerInventory: IInventory, initialX: Int, initialY: Int, hotBarGap: Int = 4, addSlotToContainer: (Slot) -> Slot) {
    (0..2).forEach { row ->
        (0..8).forEach { column ->
            val x = initialX + column * 18
            val y = initialY + row * 18
            addSlotToContainer(Slot(playerInventory, column + row * 9 + 10, x, y))
        }
    }
    (0..8).forEach { column ->
        val x = initialX + column * 18
        val y = initialY + 3 * 18 + hotBarGap
        addSlotToContainer(Slot(playerInventory, column, x, y))
    }
}
