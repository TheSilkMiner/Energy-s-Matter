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

package net.thesilkminer.mc.ematter.common.recipe.mad.step

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.crafting.IRecipe
import net.thesilkminer.mc.ematter.common.recipe.mad.capability.craftedMadRecipesAmountCapability

@ExperimentalUnsignedTypes
interface SteppingFunction {
    fun getPowerCostAt(x: Long): ULong
    fun getPowerCostFor(player: EntityPlayer, recipe: IRecipe) = this.getPowerCostAt(player.getCapability(craftedMadRecipesAmountCapability, null)!!.findAmountFor(recipe))
}
