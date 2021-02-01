/*
 * Copyright (C) 2020  TheSilkMiner
 *
 * This file is part of Energy's Matter.
 *
 * Energy's Matter is provided AS IS, WITHOUT ANY WARRANTY, even without the
 * implied warranty of FITNESS FOR A CERTAIN PURPOSE. Energy's Matter is
 * therefore being distributed in the hope it will be useful, but no
 * other assumptions are made.
 *
 * Energy's Matter is considered "all rights reserved", meaning you are not
 * allowed to copy or redistribute any part of this program, including
 * but not limited to the compiled binaries, the source code, or any
 * other form of the program without prior written permission of the
 * owner.
 *
 * On the other hand, you are allowed as per terms of GitHub to fork
 * this repository and produce derivative works, as long as they remain
 * for PERSONAL USAGE only: redistribution of changed binaries is also
 * not allowed.
 *
 * Refer to the 'COPYING' file in this repository for more information
 *
 * Contact information:
 * E-mail: thesilkminer <at> outlook <dot> com
 */

@file:JvmName("TTCS@")

package net.thesilkminer.mc.ematter.common.system.temperature.condition

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
    val and = temperatureTableConditionSerializerDeferredRegister.register("and", ::AndConditionSerializer)
    val biome = temperatureTableConditionSerializerDeferredRegister.register("biome", ::BiomeConditionSerializer)
    val biomeDictionaryType = temperatureTableConditionSerializerDeferredRegister.register("biome_dictionary_type", ::BiomeDictionaryTypeConditionSerializer)
    val biomeProperties = temperatureTableConditionSerializerDeferredRegister.register("biome_properties", ::BiomePropertiesConditionSerializer)
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
