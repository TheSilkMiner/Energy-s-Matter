@file:JvmName("TTCS")

package net.thesilkminer.mc.ematter.common.temperature.condition

import com.google.gson.JsonObject
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryManager
import net.thesilkminer.mc.ematter.common.temperature.TemperatureContext

internal val temperatureTableConditionSerializerRegistry by lazy { RegistryManager.ACTIVE.getRegistry(TemperatureTableConditionSerializer::class.java) }

interface TemperatureTableConditionSerializer : IForgeRegistryEntry<TemperatureTableConditionSerializer> {
    fun read(json: JsonObject): (TemperatureContext) -> Boolean
}
