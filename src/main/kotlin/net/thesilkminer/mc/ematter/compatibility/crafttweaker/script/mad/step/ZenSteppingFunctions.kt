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

@file:JvmName("ZSF")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.script.mad.step

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import crafttweaker.CraftTweakerAPI
import crafttweaker.annotations.ZenRegister
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.api.log.L
import net.thesilkminer.mc.boson.compatibility.crafttweaker.zenscriptx.function.LongUnaryOperator
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.MOD_NAME
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.common.recipe.mad.step.get
import net.thesilkminer.mc.ematter.common.recipe.mad.step.steppingFunctionSerializerRegistry
import stanhebben.zenscript.annotations.Optional
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod
import kotlin.math.E

@ExperimentalUnsignedTypes
@Suppress("unused")
@ZenClass("mods.ematter.mad.step.SteppingFunctions")
@ZenRegister
internal object ZenSteppingFunctions {
    @ZenMethod("constant")
    @JvmStatic
    fun constant(constant: Long): ZenSteppingFunction? {
        val json = json { this.add("value", constant) }
        return wrap(withCatchingJson("constant", json), "SteppingFunctions.constant($constant)")
    }

    @ZenMethod("exponential")
    @JvmStatic
    fun exponential(@Optional(valueDouble = E) base: Double, @Optional(valueLong = 1L) coefficient: Long, @Optional(valueLong = 0L) translation: Long,
                    @Optional(valueBoolean = false) mirror: Boolean) : ZenSteppingFunction? {
        val json = json {
            this.add("base", base)
            this.add("coefficient", coefficient)
            this.add("mirror", mirror)
            this.add("translation", translation)
        }
        return wrap(withCatchingJson("exponential", json), "SteppingFunctions.exponential($base, $coefficient, $translation, $mirror)")
    }

    @ZenMethod("linear")
    @JvmStatic
    fun linear(slope: Long, @Optional(valueLong = 0L) intercept: Long): ZenSteppingFunction? {
        val json = json {
            this.add("slope", slope)
            this.add("intercept", intercept)
        }
        return wrap(withCatchingJson("linear", json), "SteppingFunctions.linear($slope, $intercept)")
    }

    @ZenMethod("quadratic")
    @JvmStatic
    fun quadratic(quadraticCoefficient: Long, @Optional(valueLong = 0L) unitCoefficient: Long, @Optional(valueLong = 0L) intercept: Long): ZenSteppingFunction? {
        val json = json {
            this.add("quadratic_coefficient", quadraticCoefficient)
            this.add("unit_coefficient", unitCoefficient)
            this.add("intercept", intercept)
        }
        return wrap(withCatchingJson("quadratic", json), "SteppingFunctions.quadratic($quadraticCoefficient, $unitCoefficient, $intercept)")
    }

    @ZenMethod("custom")
    @JvmStatic
    fun zen(operator: LongUnaryOperator): ZenSteppingFunction = ZenCustomSteppingFunction(operator)

    @ZenMethod("custom___unsafe")
    @JvmStatic
    fun unsafeZen(operator: LongUnaryOperator): ZenSteppingFunction = ZenCustomSteppingFunction(operator, isSafe = false)
}

private inline fun json(block: JsonObject.() -> Unit) = JsonObject().apply(block)
@Suppress("NOTHING_TO_INLINE") private inline fun JsonObject.add(property: String, value: Boolean) = this.add(property, JsonPrimitive(value))
@Suppress("NOTHING_TO_INLINE") private inline fun JsonObject.add(property: String, value: Double) = this.add(property, JsonPrimitive(value))
@Suppress("NOTHING_TO_INLINE") private inline fun JsonObject.add(property: String, value: Long) = this.add(property, JsonPrimitive(value))


@ExperimentalUnsignedTypes
private fun wrap(steppingFunction: SteppingFunction?, commandString: String): ZenSteppingFunction? = steppingFunction?.let { ZenWrappingSteppingFunction(it, commandString) }

@ExperimentalUnsignedTypes
private fun withCatchingJson(name: String, json: JsonObject, type: String = name) = try { withJson(name, json) } catch (e: JsonParseException) { log(e, type).let { null } }

@ExperimentalUnsignedTypes private fun withJson(name: String, json: JsonObject) = withJson(NameSpacedString(MOD_ID, name), json)
@ExperimentalUnsignedTypes private fun withJson(serializerName: NameSpacedString, json: JsonObject) = steppingFunctionSerializerRegistry[serializerName].read(json)

private fun log(e: JsonParseException, type: String) = CraftTweakerAPI.logError("Unable to create a $type stepping function with the given arguments: ${e.message}", e)

@ExperimentalUnsignedTypes
private class ZenWrappingSteppingFunction(private val wrapped: SteppingFunction, private val commandString: String) : ZenSteppingFunction {
    override fun getPowerCostAt(x: Long) = this.wrapped.getPowerCostAt(x)
    override fun zenGetPowerCostAt(x: Long) = this.getPowerCostAt(x).toLong()
    override fun toCommandString() = this.commandString
}

@ExperimentalUnsignedTypes
private class ZenCustomSteppingFunction private constructor(private val function: (Long) -> Long, private val isSafe: Boolean) : ZenSteppingFunction {
    constructor(function: LongUnaryOperator, isSafe: Boolean) : this({ function.applyAsLong(it) }, isSafe)
    constructor(function: LongUnaryOperator) : this(function, isSafe = true)

    companion object {
        private val l = L(MOD_NAME, "CraftTweaker Integration")
    }

    override fun zenGetPowerCostAt(x: Long): Long {
        val returnValue = this.function(x)
        if (returnValue <= 0) {
            if (this.isSafe) throw UnsupportedOperationException("A power cost specified through a custom zen stepping function ended up in the negatives ($returnValue)")
            l.error("A power cost specified through a custom zen stepping function ended up in the negatives ($returnValue), but we're in UNSAFE MODE; DO NOT COME TO US WITH ISSUES")
        }
        return returnValue
    }

    override fun toCommandString() = "SteppingFunctions.custom${if (this.isSafe) "" else "___unsafe"}(function (x as long) as long { ... })"
    override val isUnsafe get() = !this.isSafe
}
