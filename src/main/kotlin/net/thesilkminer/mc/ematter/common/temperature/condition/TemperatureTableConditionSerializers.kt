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

internal object TemperatureTableConditionSerializers {

}

internal fun attachTemperatureTableConditionSerializersListener(bus: EventBus) =
        temperatureTableConditionSerializerDeferredRegister.subscribeOnto(bus).also { TemperatureTableConditionSerializers.toString() } // Static init
