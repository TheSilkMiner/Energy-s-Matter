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

@file:JvmName("WRU")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step

import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction

@ExperimentalUnsignedTypes
internal fun SteppingFunction.toCommandString() = if (this is ZenSteppingFunction) this.toCommandString() else this.nativeToCommandString()

@ExperimentalUnsignedTypes
private fun SteppingFunction.nativeToCommandString() = this.tryNativeCommandString() ?: "SteppingFunctions.custom(function (x as double) as double { ... })"

@ExperimentalUnsignedTypes
private fun SteppingFunction.tryNativeCommandString() = when (this::class.simpleName) {
    null -> null
    "ConstantSteppingFunction" -> check { this.toConstantCommandString() }
    "ExponentialSteppingFunction" -> check { this.toExponentialCommandString() }
    "LinearSteppingFunction" -> check { this.toLinearCommandString() }
    "QuadraticSteppingFunction" -> check { this.toQuadraticCommandString() }
    else -> null
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toConstantCommandString(): String {
    // ULongs, being inline classes, don't exist at runtime, so they get compiled down to longs. Nevertheless, better safe than sorry
    val cost = try { this.field<Long>("cost").toULong() } catch(e: ClassCastException) { this.field<ULong>("cost") }
    return "SteppingFunctions.constant($cost)"
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toExponentialCommandString(): String {
    val coefficient = this.field<Long>("coefficient")
    val base = this.field<Double>("base")
    val mirror = this.field<Boolean>("mirror")
    val translation = this.field<Long>("translation")
    return "SteppingFunctions.exponential($base, $coefficient, $translation, $mirror)"
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toLinearCommandString(): String {
    val slope = this.field<Long>("slope")
    val intercept = this.field<Long>("intercept")
    return "SteppingFunctions.linear($slope, $intercept)"
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toQuadraticCommandString(): String {
    val quadraticCoefficient = this.field<Long>("quadraticCoefficient")
    val unitCoefficient = this.field<Long>("unitCoefficient")
    val intercept = this.field<Long>("intercept")
    return "SteppingFunctions.quadratic($quadraticCoefficient, $unitCoefficient, $intercept)"
}

@JvmName("\$") private fun check(block: () -> String): String? = try { block() } catch (e: ReflectiveOperationException) { null }

@ExperimentalUnsignedTypes
private inline fun <reified T> SteppingFunction.field(name: String) = this::class.java.getDeclaredField(name).apply { this.isAccessible = true }[this].uncheckedCast<T>()
