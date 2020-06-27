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
