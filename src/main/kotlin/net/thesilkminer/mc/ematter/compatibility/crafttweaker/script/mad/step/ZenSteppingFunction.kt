package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step

import crafttweaker.annotations.ZenRegister
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ExperimentalUnsignedTypes
@ZenClass("net.thesilkminer.mc.ematter.zen.mad.step.SteppingFunction")
@ZenRegister
internal interface ZenSteppingFunction : SteppingFunction {
    override fun getPowerCostAt(x: Long): ULong = this.zenGetPowerCostAt(x).toULong()

    @ZenMethod("getPowerCostAt") fun zenGetPowerCostAt(x: Long): Long
    @ZenMethod("toCommandString") fun toCommandString(): String

    val isUnsafe get() = false
}
