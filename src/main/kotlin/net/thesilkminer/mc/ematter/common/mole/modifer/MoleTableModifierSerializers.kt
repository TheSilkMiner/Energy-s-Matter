package net.thesilkminer.mc.ematter.common.mole.modifer

import net.minecraftforge.fml.common.eventhandler.EventBus
import net.thesilkminer.mc.boson.api.registry.DeferredRegister
import net.thesilkminer.mc.ematter.MOD_ID

private const val REGISTRY_NAME = "mole_table_modifier_serializers"

private val moleTableModifierSerializerDeferredRegister =
    DeferredRegister(MOD_ID, MoleTableModifierSerializer::class, REGISTRY_NAME) {
        this.setMaxID(67_108_863).disableSaving().allowModification()
    }

@Suppress("unused")
internal object MoleTableModifierSerializers {
    val plus = moleTableModifierSerializerDeferredRegister.register("plus", ::MoleAdditionModifierSerializer)
    val minus = moleTableModifierSerializerDeferredRegister.register("minus", ::MoleSubtractionModifierSerializer)
    val times = moleTableModifierSerializerDeferredRegister.register("times", ::MoleMultiplicationModifierSerializer)
    val div = moleTableModifierSerializerDeferredRegister.register("div", ::MoleDivisionModifierSerializer)
}

internal fun attachMoleTableModifierSerializersListener(bus: EventBus) =
    moleTableModifierSerializerDeferredRegister.subscribeOnto(bus).also { MoleTableModifierSerializers.toString() } // Static init
