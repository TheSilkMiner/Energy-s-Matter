@file:JvmName("TTCS@")

package net.thesilkminer.mc.ematter.common.temperature.condition

import net.minecraftforge.fml.common.eventhandler.EventBus
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.ematter.MOD_ID

private const val REGISTRY_NAME = "temperature_table_condition_serializers"

private val temperatureTableConditionSerializerDeferredRegister =
        DeferredRegister(MOD_ID, TemperatureTableConditionSerializer::class, REGISTRY_NAME) {
            this.setMaxID(67_108_863).disableSaving().allowModification()
        }

@Suppress("unused")
internal object TemperatureTableConditionSerializers {
    val biome = temperatureTableConditionSerializerDeferredRegister.register("biome", ::BiomeConditionSerializer)
    @Suppress("SpellCheckingInspection")
    val blockStateProperty = temperatureTableConditionSerializerDeferredRegister.register("blockstate_property", ::BlockStatePropertyConditionSerializer)
    val dimension = temperatureTableConditionSerializerDeferredRegister.register("dimension", ::DimensionConditionSerializer)
    val not = temperatureTableConditionSerializerDeferredRegister.register("not", ::NotConditionSerializer)
    val or = temperatureTableConditionSerializerDeferredRegister.register("or", ::OrConditionSerializer)
    val position = temperatureTableConditionSerializerDeferredRegister.register("position", ::PositionConditionSerializer)
    val timeOfDay = temperatureTableConditionSerializerDeferredRegister.register("time_of_day", ::DayTimeConditionSerializer)
}

internal fun attachTemperatureTableConditionSerializersListener(bus: EventBus) =
        temperatureTableConditionSerializerDeferredRegister.subscribeOnto(bus).also { TemperatureTableConditionSerializers.toString() } // Static init
