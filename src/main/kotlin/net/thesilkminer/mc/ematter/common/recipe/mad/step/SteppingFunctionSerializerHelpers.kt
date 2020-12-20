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

@file:JvmName("SFSH")

package net.thesilkminer.mc.ematter.common.recipe.mad.step

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation

internal fun JsonObject.long(name: String, default: Long? = null): Long {
    if (!this.has(name)) {
        if (default != null) return default
        throw JsonSyntaxException("Expected '$name' to be a number, but instead the member was missing")
    }
    val element = this[name]
    if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) throw JsonSyntaxException("Expected '$name' to be a number, but instead found '${JsonUtils.toString(element)}'")
    return element.asJsonPrimitive.asLong
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal operator fun IForgeRegistry<SteppingFunctionSerializer>.get(serialized: JsonObject) = this[JsonUtils.getString(serialized, "type")]

@Suppress("EXPERIMENTAL_API_USAGE")
internal operator fun IForgeRegistry<SteppingFunctionSerializer>.get(name: String) = this[name.toNameSpacedString()]

@Suppress("EXPERIMENTAL_API_USAGE")
internal operator fun IForgeRegistry<SteppingFunctionSerializer>.get(name: NameSpacedString) =
        this.getValue(name.toResourceLocation()) ?: throw IllegalStateException("No serializer with the name '$name' exists")
