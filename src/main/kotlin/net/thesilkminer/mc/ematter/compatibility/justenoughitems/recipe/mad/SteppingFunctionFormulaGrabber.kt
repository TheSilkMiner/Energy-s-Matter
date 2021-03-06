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

@file:JvmName("SFFG")

package net.thesilkminer.mc.ematter.compatibility.justenoughitems.recipe.mad

import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import java.lang.ClassCastException
import java.lang.reflect.Field
import kotlin.reflect.KClass

private val fieldCache = mutableMapOf<KClass<*>, MutableMap<String, Field>>()

@ExperimentalUnsignedTypes
internal fun SteppingFunction.grabFormula() = this.obtainFormulaReflectively()

@ExperimentalUnsignedTypes
private fun SteppingFunction.obtainFormulaReflectively(): List<String>? = when(this::class.simpleName) {
    null -> null
    "FakeDoNothingSteppingFunction" -> listOf("y = 0")
    "ConstantSteppingFunction" -> check { this.toConstantFormula() }
    "ExponentialSteppingFunction" -> check { this.toExponentialFormula() }
    "LinearSteppingFunction" -> check { this.toLinearFormula() }
    "QuadraticSteppingFunction" -> check { this.toQuadraticFormula() }
    "ZenWrappingSteppingFunction" -> check { this.unwrap().grabFormula() }
    else -> null
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toConstantFormula(): List<String> {
    // ULongs don't exist at runtime because they are inlined, but better safe than sorry I guess
    val cost = try { this.field<Long>("cost").toULong() } catch (e: ClassCastException) { this.field<ULong>("cost") }
    return listOf("y = $cost")
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toExponentialFormula(): List<String> {
    val coefficient = this.field<Long>("coefficient")
    val base = this.field<Double>("base")
    val mirror = this.field<Boolean>("mirror")
    val translation = this.field<Long>("translation")
    return listOf("y = $coefficient(${"%.1f".format(base)}^m)", "m = ${if (mirror) "-(" else ""}x - $translation${if (mirror) ")" else ""}")
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toLinearFormula(): List<String> {
    val slope = this.field<Long>("slope")
    val intercept = this.field<Long>("intercept")
    return listOf("y = ${slope}x + $intercept")
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.toQuadraticFormula(): List<String> {
    val quadraticCoefficient = this.field<Long>("quadraticCoefficient")
    val unitCoefficient = this.field<Long>("unitCoefficient")
    val intercept = this.field<Long>("intercept")
    return listOf("y = ${quadraticCoefficient}(x^2)", "     + ${unitCoefficient}x", "     + $intercept")
}

@ExperimentalUnsignedTypes
private fun SteppingFunction.unwrap() = this.field<SteppingFunction>("wrapped")

@JvmName("\$") private fun check(block: () -> List<String>?): List<String>? = try { block() } catch (e: ReflectiveOperationException) { listOf() }

@ExperimentalUnsignedTypes
private inline fun <reified T> SteppingFunction.field(name: String): T {
    val fields = fieldCache.computeIfAbsent(this::class) { mutableMapOf() }
    val field = fields.computeIfAbsent(name) { this::class.java.getDeclaredField(name).apply { this.isAccessible = true } }
    return field[this].uncheckedCast()
}
