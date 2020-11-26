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

@file:JvmName("CE")

package net.thesilkminer.mc.ematter.common.shared

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
