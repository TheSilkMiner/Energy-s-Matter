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
    val constant = steppingFunctionSerializersDeferredRegister.register("constant") { ConstantSteppingFunctionSerializer() }
    val exponential = steppingFunctionSerializersDeferredRegister.register("exponential") { ExponentialSteppingFunctionSerializer() }
    val linear = steppingFunctionSerializersDeferredRegister.register("linear") { LinearSteppingFunctionSerializer() }
    val piecewise = steppingFunctionSerializersDeferredRegister.register("piecewise") { LinearSteppingFunctionSerializer() }
    val quadratic = steppingFunctionSerializersDeferredRegister.register("quadratic") { QuadraticSteppingFunctionSerializer() }
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal fun attachSteppingFunctionListener(bus: EventBus) = steppingFunctionSerializersDeferredRegister.subscribeOnto(bus).also { SteppingFunctionSerializers.toString() } // Static init
