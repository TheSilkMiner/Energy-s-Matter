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

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step

import crafttweaker.annotations.ZenRegister
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ExperimentalUnsignedTypes
@ZenClass("mods.ematter.mad.step.SteppingFunction")
@ZenRegister
internal interface ZenSteppingFunction : SteppingFunction {
    override fun getPowerCostAt(x: Long): ULong = this.zenGetPowerCostAt(x).toULong()

    @ZenMethod("getPowerCostAt") fun zenGetPowerCostAt(x: Long): Long
    @ZenMethod("toCommandString") fun toCommandString(): String

    val isUnsafe get() = false
}
