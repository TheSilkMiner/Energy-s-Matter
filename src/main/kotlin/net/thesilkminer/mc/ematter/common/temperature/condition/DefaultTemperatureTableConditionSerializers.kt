@file:JvmName("DTTCS")

package net.thesilkminer.mc.ematter.common.temperature.condition

import com.google.common.collect.HashBiMap
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.util.JsonUtils
import net.minecraft.util.math.BlockPos
import net.minecraft.world.biome.Biome
import net.minecraftforge.common.BiomeDictionary
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.registries.IForgeRegistryEntry
import net.thesilkminer.kotlin.commons.lang.uncheckedCast
import net.thesilkminer.mc.boson.prefab.naming.toNameSpacedString
import net.thesilkminer.mc.boson.prefab.naming.toResourceLocation
import net.thesilkminer.mc.ematter.common.temperature.TemperatureContext
import java.lang.ClassCastException
import java.util.Locale
import kotlin.math.abs
import kotlin.reflect.full.isSubclassOf

internal class BiomeConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val biomeName = JsonUtils.getString(json, "registry_name")
        val biome = ForgeRegistries.BIOMES.getValue(biomeName.toNameSpacedString().toResourceLocation()) ?: throw JsonParseException("No such biome '$biomeName' exists")

        return { it.world.getBiome(it.pos) == biome }
    }
}

internal class BiomeDictionaryTypeConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    override fun read(json: JsonObject) = if (json.has("dictionary_types")) this.parseTypes(json) else this.parseType(json)

    private fun parseTypes(json: JsonObject): (TemperatureContext) -> Boolean =
            JsonUtils.getJsonArray(json, "dictionary_types")
                    .asSequence()
                    .mapIndexed { index, element -> JsonUtils.getString(element, "dictionary_types[$index]") }
                    .map(this::parseType)
                    .toList()
                    .let { list -> { context -> list.all { function -> function(context) } } }

    private fun parseType(json: JsonObject): (TemperatureContext) -> Boolean = this.parseType(JsonUtils.getString(json, "dictionary_type"))

    private fun parseType(type: String): (TemperatureContext) -> Boolean {
        val targetType = BiomeDictionary.Type.getAll().find { it.name.toLowerCase(Locale.ROOT) == type } ?: throw JsonParseException("No such biome type '$type' exists")

        return { targetType in BiomeDictionary.getTypes(it.world.getBiome(it.pos)) }
    }
}

internal class BiomePropertiesConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    companion object {
        private val temperatureCategoryByName =
                mapOf("ocean" to Biome.TempCategory.OCEAN, "warm" to Biome.TempCategory.WARM, "medium" to Biome.TempCategory.MEDIUM, "cold" to Biome.TempCategory.COLD)
    }

    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val temperature = if (json.has("base_temperature")) JsonUtils.getFloat(json, "base_temperature") else null
        val temperatureCategoryName = if (json.has("temperature_category")) JsonUtils.getString(json, "temperature_category") else null
        val rain = if (json.has("rainfall_level")) JsonUtils.getFloat(json, "rain") else null

        val temperatureCategory = temperatureCategoryName?.let { temperatureCategoryByName[it] }

        if (listOfNotNull(temperature, rain, temperatureCategory).isEmpty()) {
            throw JsonSyntaxException("You must specify at least one biome parameter between 'base_temperature', 'temperature_category', 'rainfall_level'")
        }

        return {
            val biome = it.world.getBiome(it.pos)
            var isValid = true

            if (temperature != null) isValid = isValid && abs(biome.defaultTemperature - temperature) <= 0.0001 // damn you floating points
            if (rain != null) isValid = isValid && abs(biome.rainfall - rain) <= 0.0001
            if (temperatureCategory != null) isValid = isValid && biome.tempCategory == temperatureCategory

            isValid
        }
    }
}

internal class BlockStatePropertyConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val propertyName = JsonUtils.getString(json, "name")
        val value = json["value"] ?: throw JsonParseException("Expected to find either 'value', but instead found nothing")

        if (value.isJsonObject || value.isJsonNull || value.isJsonArray) throw JsonParseException("Property value must be a primitive")

        return {
            val state = it.world.getBlockState(it.pos)
            val targetProperty = state.propertyKeys.first { property -> property.getName() == propertyName }
            try {
                value.asJsonPrimitive.let { target ->
                    when {
                        target.isNumber -> this.checkNumberProperty(state, targetProperty, target.asNumber)
                        target.isBoolean -> this.checkBooleanProperty(state, targetProperty, target.asBoolean)
                        target.isString -> this.checkStringProperty(state, targetProperty, target.asString)
                        else -> throw IllegalArgumentException("A JSON primitive must be a primitive, but it isn't! What is this sorcery? $target")
                    }
                }
            } catch (e: ClassCastException) {
                throw JsonSyntaxException("Attempted to query a value of an invalid type for property '$targetProperty'", e)
            }
        }
    }

    private fun checkBooleanProperty(state: IBlockState, property: IProperty<*>, value: Boolean): Boolean {
        if (property.getValueClass().kotlin != Boolean::class) this.invalidQuery("boolean", property)
        val propertyValue = state.getValue(property.toComparable()).uncheckedCast<Boolean>()
        return value == propertyValue
    }

    private fun checkNumberProperty(state: IBlockState, property: IProperty<*>, value: Number): Boolean {
        if (!property.getValueClass().kotlin.isSubclassOf(Number::class)) this.invalidQuery("numerical", property)
        val propertyValue = state.getValue(property.toComparable()).uncheckedCast<Comparable<Number>>()
        return this.checkWithCompare(propertyValue, value)
    }

    private fun checkStringProperty(state: IBlockState, property: IProperty<*>, value: String): Boolean {
        if (property.getValueClass().kotlin.isSubclassOf(Enum::class)) return this.checkEnumProperty(state, property, value)
        if (property.getValueClass().kotlin != String::class) this.invalidQuery("String", property)
        val propertyValue = state.getValue(property.toComparable()).uncheckedCast<Comparable<String>>()
        return this.checkWithCompare(propertyValue, value)
    }

    private fun checkEnumProperty(state: IBlockState, property: IProperty<*>, value: String): Boolean {
        val enumValue = property.parseValue(value).let {
            if (!it.isPresent) throw JsonSyntaxException("Attempted to query value '$value', but it does not represent a valid constant in '${property.getValueClass().kotlin.qualifiedName}'")
        }.uncheckedCast<Enum<*>>()
        val propertyValue = state.getValue(property.toComparable()).uncheckedCast<Comparable<Enum<*>>>()
        return this.checkWithCompare(propertyValue, enumValue)
    }

    private fun <T> checkWithCompare(propertyValue: Comparable<T>, other: T) = propertyValue.compareTo(other) == 0

    private fun invalidQuery(type: String, property: IProperty<*>): Nothing =
            throw JsonSyntaxException("Attempted to query a $type value for a property with of type '${property.getValueClass().kotlin.qualifiedName}' in a temperature table")

    private fun IProperty<*>.toComparable() = this.uncheckedCast<IProperty<Comparable<Any>>>()
}

internal class DayTimeConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    private companion object {
        private val dayMomentMap = HashBiMap.create<TemperatureContext.DayMoment, String>().apply {
            this[TemperatureContext.DayMoment.DAWN] = "dawn"
            this[TemperatureContext.DayMoment.DAY] = "day"
            this[TemperatureContext.DayMoment.DUSK] = "dusk"
            this[TemperatureContext.DayMoment.NIGHT] = "night"

            this.inverse() // Generate inverse on load rather than lazily
        }
    }

    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val time = JsonUtils.getString(json, "time")
        val moment = dayMomentMap.inverse()[time]
        return { it.dayMoment == moment }
    }
}

internal class DimensionConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val type = if (json.has("type")) JsonUtils.getString(json, "type") else null
        val id = if (json.has("id")) JsonUtils.getInt(json, "id") else null

        if (type == null && id == null) throw JsonSyntaxException("You must specify either the type, or the id for the dimension")
        if (type != null && id != null) throw JsonSyntaxException("You cannot specify both the dimension type and the dimension ID: they're redundant")

        return if (type != null) { { it.world.provider.dimensionType.getName() == type } } else { { it.world.provider.dimension == id } }
    }
}

internal class NotConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    override fun read(json: JsonObject): (TemperatureContext) -> Boolean =
            (JsonUtils.getJsonObject(json, "condition").let { temperatureTableConditionSerializerRegistry[it].read(it) }).let { cond -> { !cond(it) } }
}

internal class OrConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val conditionsArray = JsonUtils.getJsonArray(json, "conditions")
        val conditions = conditionsArray.asSequence()
                .mapIndexed { index, array -> JsonUtils.getJsonObject(array, "conditions[$index]") }
                .map { temperatureTableConditionSerializerRegistry[it].read(it) }
                .toList()

        return { conditions.any { condition -> condition(it) } }
    }
}

internal class PositionConditionSerializer : IForgeRegistryEntry.Impl<TemperatureTableConditionSerializer>(), TemperatureTableConditionSerializer {
    @Suppress("unused")
    private enum class Conditions(val jsonName: String, val carryOutOperation: (Int, Axis, BlockPos) -> Boolean) {
        LESS_THAN("less_than", { target, axis, pos -> axis.grabAxis(pos) < target }),
        GREATER_THAN("greater_than", { target, axis, pos -> axis.grabAxis(pos) > target }),
        LESS_THAN_OR_EQUAL_TO("matches_or_less", { target, axis, pos -> axis.grabAxis(pos) <= target }),
        GREATER_THAN_OR_EQUAL_TO("matches_or_more", { target, axis, pos -> axis.grabAxis(pos) >= target }),
        EQUAL_TO("matches", { target, axis, pos -> axis.grabAxis(pos) == target });

        companion object {
            val VALUES = values().toList()
        }
    }

    @Suppress("unused")
    private enum class Axis(val jsonName: String, val grabAxis: (BlockPos) -> Int) {
        X("x", { it.x }),
        Y("y", { it.y }),
        Z("z", { it.z });

        companion object {
            val VALUES = values().toList()
        }
    }

    override fun read(json: JsonObject): (TemperatureContext) -> Boolean {
        val condition = JsonUtils.getString(json, "condition").let { cond ->
            Conditions.VALUES.find { cond == it.jsonName } ?: throw JsonSyntaxException("No such condition '$cond' exists: allowed values are '${Conditions.VALUES.map { it.jsonName }}")
        }
        val axis = JsonUtils.getString(json, "axis").let { axis ->
            Axis.VALUES.find { axis == it.jsonName } ?: throw JsonSyntaxException("No such axis '$axis' exists: allowed values are '${Axis.VALUES.map { it.jsonName }}")
        }
        val target = JsonUtils.getInt(json, "value")
        return { condition.carryOutOperation(target, axis, it.pos) }
    }
}
