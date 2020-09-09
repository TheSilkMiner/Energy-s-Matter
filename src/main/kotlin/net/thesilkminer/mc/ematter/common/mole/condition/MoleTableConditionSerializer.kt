@file:JvmName("MTCS")

package net.thesilkminer.mc.ematter.common.mole.condition

import com.google.gson.JsonObject
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryManager
import net.thesilkminer.mc.ematter.common.mole.MoleContext

internal val moleTableConditionSerializerRegistry by lazy { RegistryManager.ACTIVE.getRegistry(MoleTableConditionSerializer::class.java) }

interface MoleTableConditionSerializer : IForgeRegistryEntry<MoleTableConditionSerializer> {
    fun read(json: JsonObject): (MoleContext) -> Boolean
}
