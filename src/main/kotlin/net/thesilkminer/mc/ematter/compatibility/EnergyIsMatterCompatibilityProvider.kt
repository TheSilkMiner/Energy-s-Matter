package net.thesilkminer.mc.ematter.compatibility

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.bosonApi
import net.thesilkminer.mc.boson.api.compatibility.CompatibilityProvider
import net.thesilkminer.mc.boson.api.event.CompatibilityProviderRegistryEvent

internal interface EnergyIsMatterCompatibilityProvider : CompatibilityProvider {
    fun onPreInitialization() {}
}

internal object CompatibilityProviderHandler {
    @SubscribeEvent
    fun onCompatibilityProviderRegistration(e: CompatibilityProviderRegistryEvent) {
        e.registry.registerProvider(EnergyIsMatterCompatibilityProvider::class)
    }

    internal fun firePreInitializationEvent() {
        bosonApi.compatibilityProviderRegistry[EnergyIsMatterCompatibilityProvider::class].forEach(EnergyIsMatterCompatibilityProvider::onPreInitialization)
    }
}
