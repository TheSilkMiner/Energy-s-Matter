@file:JvmName("MTCS@")

package net.thesilkminer.mc.ematter.common.mole.condition

import net.minecraftforge.fml.common.eventhandler.EventBus
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.ematter.MOD_ID

private const val REGISTRY_NAME = "mole_table_condition_serializers"

private val moleTableConditionSerializerDeferredRegister =
    DeferredRegister(MOD_ID, MoleTableConditionSerializer::class, REGISTRY_NAME) {
        this.setMaxID(67_108_863).disableSaving().allowModification()
    }

@Suppress("unused")
internal object MoleTableConditionSerializers {
    val and = moleTableConditionSerializerDeferredRegister.register("and", ::MoleAndConditionSerializer)
    val not = moleTableConditionSerializerDeferredRegister.register("not", ::MoleNotConditionSerializer)
    val or = moleTableConditionSerializerDeferredRegister.register("or", ::MoleOrConditionSerializer)
    val meta = moleTableConditionSerializerDeferredRegister.register("meta", ::MoleMetadataConditionSerializer)
    val durability = moleTableConditionSerializerDeferredRegister.register("durability", ::MoleDurabilityConditionSerializer)
}

internal fun attachMoleTableConditionSerializersListener(bus: EventBus) =
    moleTableConditionSerializerDeferredRegister.subscribeOnto(bus).also { MoleTableConditionSerializers.toString() } // Static init
