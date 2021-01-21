package net.thesilkminer.mc.ematter.common.mole.modifer

import com.google.gson.JsonObject
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryManager
import net.thesilkminer.mc.ematter.common.mole.MoleContext
import net.thesilkminer.mc.ematter.common.mole.Moles

internal val moleTableModifierSerializerRegistry by lazy { RegistryManager.ACTIVE.getRegistry(MoleTableModifierSerializer::class.java) }

interface MoleTableModifierSerializer : IForgeRegistryEntry<MoleTableModifierSerializer> {
    fun read(json: JsonObject): (MoleContext, Moles) -> Moles
}
