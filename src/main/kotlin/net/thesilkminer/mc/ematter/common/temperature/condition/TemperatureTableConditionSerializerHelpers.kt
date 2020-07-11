@file:JvmName("TTCSH")

package net.thesilkminer.mc.ematter.common.temperature.condition

import com.google.gson.JsonObject
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation

private typealias Reg = IForgeRegistry<TemperatureTableConditionSerializer>

internal operator fun Reg.get(serialized: JsonObject) = this[JsonUtils.getString(serialized, "type")]
internal operator fun Reg.get(name: String) = this[name.toNameSpacedString()]
internal operator fun Reg.get(name: NameSpacedString) = this.getValue(name.toResourceLocation())
        ?: throw IllegalStateException("No serializer with name '$name' exist")
