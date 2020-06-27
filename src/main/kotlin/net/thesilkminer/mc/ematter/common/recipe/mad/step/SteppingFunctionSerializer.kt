@file:JvmName("SFS")

package net.thesilkminer.mc.ematter.common.recipe.mad.step

import com.google.gson.JsonObject
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryManager
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation

@Suppress("EXPERIMENTAL_API_USAGE")
internal val steppingFunctionSerializerRegistry by lazy { RegistryManager.ACTIVE.getRegistry(SteppingFunctionSerializer::class.java) }

@ExperimentalUnsignedTypes
interface SteppingFunctionSerializer : IForgeRegistryEntry<SteppingFunctionSerializer> {
    fun read(json: JsonObject): SteppingFunction
}
