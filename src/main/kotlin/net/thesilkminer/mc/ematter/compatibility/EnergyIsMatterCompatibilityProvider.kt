package net.thesilkminer.mc.ematter.compatibility

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.compatibility.CompatibilityProvider
import net.thesilkminer.mc.boson.api.event.CompatibilityProviderRegistryEvent

internal interface EnergyIsMatterCompatibilityProvider : CompatibilityProvider

internal object CompatibilityProviderHandler {
    @SubscribeEvent
    fun onCompatibilityProviderRegistration(e: CompatibilityProviderRegistryEvent) {
        e.registry.registerProvider(EnergyIsMatterCompatibilityProvider::class)
    }
}
