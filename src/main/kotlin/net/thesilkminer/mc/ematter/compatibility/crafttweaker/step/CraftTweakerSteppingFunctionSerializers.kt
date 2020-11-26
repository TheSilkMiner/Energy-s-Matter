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

@file:JvmName("CTSFS")

package net.thesilkminer.mc.ematter.compatibility.crafttweaker.step

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import net.minecraft.util.JsonUtils
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.boson.api.modid.CRAFT_TWEAKER_2
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunction
import net.thesilkminer.mc.ematter.common.recipe.mad.step.SteppingFunctionSerializer
import net.thesilkminer.mc.ematter.compatibility.crafttweaker.compiler.compileFunction

@ExperimentalUnsignedTypes
private val crtSteppingFunctionSerializerRegistry = DeferredRegister(CRAFT_TWEAKER_2, SteppingFunctionSerializer::class)

@ExperimentalUnsignedTypes
@Suppress("unused")
internal object CraftTweakerSteppingFunctionSerializers {
    val zs = crtSteppingFunctionSerializerRegistry.register("zenscript", ::ZenScriptBasedSteppingFunctionSerializer)
}

@ExperimentalUnsignedTypes
internal class ZenScriptBasedSteppingFunctionSerializer : IForgeRegistryEntry.Impl<SteppingFunctionSerializer>(), SteppingFunctionSerializer {
    private class ZenScriptBasedSteppingFunction(val expression: (Long) -> ULong) : SteppingFunction {
        override fun getPowerCostAt(x: Long): ULong = this.expression(x)
    }

    override fun read(json: JsonObject): SteppingFunction {
        if (!json.has("expression")) throw JsonSyntaxException("Missing 'expression': this is required for a ZS-based stepping function")
        val expression = JsonUtils.getString(json, "expression").fixUp()
        return ZenScriptBasedSteppingFunction(expression.compile().let { function -> { value: Long -> function(value).toULong() } })
    }

    private fun String.fixUp() = "function __(x as double) as double { return $this ;}"

    private fun String.compile() = try {
        compileFunction(this)
    } catch (e: Exception) {
        throw JsonParseException("An error has occurred while trying to compile the given expression '$this'", e)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal fun attachCraftTweakerSteppingFunctionSerializerRegistry(bus: EventBus) =
        crtSteppingFunctionSerializerRegistry.subscribeOnto(bus).also { CraftTweakerSteppingFunctionSerializers.toString() }
