package net.thesilkminer.mc.ematter.common.mole.modifer

import com.google.gson.JsonObject
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistry
import net.thesilkminer.mc.boson.api.id.NameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation

private typealias MoleReg = IForgeRegistry<MoleTableModifierSerializer>

internal operator fun MoleReg.get(serialized: JsonObject) = this[JsonUtils.getString(serialized, "type")]
internal operator fun MoleReg.get(name: String) = this[name.toNameSpacedString()]
internal operator fun MoleReg.get(name: NameSpacedString) = this.getValue(name.toResourceLocation())
    ?: throw IllegalStateException("No serializer with name '$name' exist")
