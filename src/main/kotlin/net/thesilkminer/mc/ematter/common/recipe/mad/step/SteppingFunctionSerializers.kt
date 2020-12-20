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

@file:JvmName("SFS@")

package net.thesilkminer.mc.ematter.common.recipe.mad.step

import net.minecraftforge.fml.common.eventhandler.EventBus
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.ematter.MOD_ID

private const val SFS_REGISTRY_NAME = "stepping_function_serializers"

@ExperimentalUnsignedTypes
private val steppingFunctionSerializersDeferredRegister = DeferredRegister(MOD_ID, SteppingFunctionSerializer::class, SFS_REGISTRY_NAME) {
    this.setMaxID(67_108_863).disableSaving().allowModification()
}

@ExperimentalUnsignedTypes
@Suppress("unused")
internal object SteppingFunctionSerializers {
    val constant = steppingFunctionSerializersDeferredRegister.register("constant", ::ConstantSteppingFunctionSerializer)
    val exponential = steppingFunctionSerializersDeferredRegister.register("exponential", ::ExponentialSteppingFunctionSerializer)
    val linear = steppingFunctionSerializersDeferredRegister.register("linear", ::LinearSteppingFunctionSerializer)
    val piecewise = steppingFunctionSerializersDeferredRegister.register("piecewise", ::PiecewiseSteppingFunctionSerializer)
    val quadratic = steppingFunctionSerializersDeferredRegister.register("quadratic", ::QuadraticSteppingFunctionSerializer)
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal fun attachSteppingFunctionListener(bus: EventBus) = steppingFunctionSerializersDeferredRegister.subscribeOnto(bus).also { SteppingFunctionSerializers.toString() } // Static init
