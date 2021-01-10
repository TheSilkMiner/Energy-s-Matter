@file:JvmName("DMTCS")

package net.thesilkminer.mc.ematter.common.mole.condition

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.util.JsonUtils
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.mc.ematter.common.mole.MoleContext
import kotlin.math.roundToInt

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

internal class MoleDurabilityConditionSerializer : IForgeRegistryEntry.Impl<MoleTableConditionSerializer>(), MoleTableConditionSerializer {

    /*
     * SingleTargetConditions:
     * {
     *   "type": "ematter:durability",
     *   "condition": String,
     *   "target": Int,
     *   "useAbsolutValues": Boolean
     * }
     *
     * RangeConditions:
     * {
     *   "type": "ematter:durability",
     *   "condition": String,
     *   "lowerTarget": Int,
     *   "higherTarget": Int,
     *   "useAbsolutValues": Boolean
     */

    private enum class SingleTargetConditions(val jsonName: String, val carryOutOperation: (Int, Int) -> Boolean) {
        LESS_THAN("less_than", { durability, target -> durability < target }),
        GREATER_THAN("more_than", { durability, target -> durability > target }),
        LESS_THAN_OR_EQUAL_TO("matches_or_less", { durability, target -> durability <= target }),
        GREATER_THAN_OR_EQUAL_TO("matches_or_more", { durability, target -> durability >= target }),
        EQUAL_TO("matches", { durability, target -> durability == target }),
    }

    private enum class RangeConditions(val jsonName: String, val carryOutOperation: (Int, Int, Int) -> Boolean) {
        IN("between", { durability, lowerTarget, higherTarget -> durability in lowerTarget..higherTarget }),
        NOT_IN("outside", { durability, lowerTarget, higherTarget -> durability !in lowerTarget..higherTarget }),  // TODO("think of a better name")
    }

    private val conditions: Set<String> = setOf(*SingleTargetConditions.values().map { it.jsonName }.toTypedArray(), *RangeConditions.values().map { it.jsonName }.toTypedArray())

    override fun read(json: JsonObject): (MoleContext) -> Boolean {
        val condition = JsonUtils.getString(json, "condition")
        val absolutValues = if (json.has("useAbsolutValues")) JsonUtils.getBoolean(json, "useAbsolutValues") else false

        SingleTargetConditions.values().find { condition == it.jsonName }?.let { cond ->
            val target = JsonUtils.getInt(json, "target")

            return { context -> cond.carryOutOperation(if (absolutValues) context.durability else ((context.durability.toDouble() / context.maxDurability.toDouble()) * 10).roundToInt() * 10, target) }
        }
        RangeConditions.values().find { condition == it.jsonName }?.let { cond ->
            val lowerTarget = JsonUtils.getInt(json, "lowerTarget")
            val higherTarget = JsonUtils.getInt(json, "higherTarget")

            return { context -> cond.carryOutOperation(if (absolutValues) context.durability else ((context.durability.toDouble() / context.maxDurability.toDouble()) * 10).roundToInt() * 10, lowerTarget, higherTarget) }
        }

        throw JsonSyntaxException("No such condition '$condition' exists: allowed values are '${this.conditions}")
    }
}
