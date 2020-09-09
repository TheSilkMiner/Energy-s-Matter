@file:JvmName("DMTCS")

package net.thesilkminer.mc.ematter.common.mole.condition

import com.google.gson.JsonObject
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.ematter.common.mole.MoleContext

internal class MoleAndConditionSerializer : IForgeRegistryEntry.Impl<MoleTableConditionSerializer>(), MoleTableConditionSerializer {
    override fun read(json: JsonObject): (MoleContext) -> Boolean {
        val conditionsArray = JsonUtils.getJsonArray(json, "conditions")
        val conditions = conditionsArray.asSequence()
                .mapIndexed { index, array -> JsonUtils.getJsonObject(array, "conditions[$index]") }
                .map { moleTableConditionSerializerRegistry[it].read(it) }
                .toList()

        return { conditions.all { condition -> condition(it) } }
    }
}

internal class MoleNotConditionSerializer : IForgeRegistryEntry.Impl<MoleTableConditionSerializer>(), MoleTableConditionSerializer {
    override fun read(json: JsonObject): (MoleContext) -> Boolean =
            (JsonUtils.getJsonObject(json, "condition").let { moleTableConditionSerializerRegistry[it].read(it) }).let { cond -> { !cond(it) } }
}

internal class MoleOrConditionSerializer : IForgeRegistryEntry.Impl<MoleTableConditionSerializer>(), MoleTableConditionSerializer {
    override fun read(json: JsonObject): (MoleContext) -> Boolean {
        val conditionsArray = JsonUtils.getJsonArray(json, "conditions")
        val conditions = conditionsArray.asSequence()
                .mapIndexed { index, array -> JsonUtils.getJsonObject(array, "conditions[$index]") }
                .map { moleTableConditionSerializerRegistry[it].read(it) }
                .toList()

        return { conditions.any { condition -> condition(it) } }
    }
}

internal class MoleMetadataConditionSerializer : IForgeRegistryEntry.Impl<MoleTableConditionSerializer>(), MoleTableConditionSerializer {
    override fun read(json: JsonObject): (MoleContext) -> Boolean {
        val meta = JsonUtils.getInt(json, "meta")
        return { context: MoleContext -> context.meta == meta }
    }
}
