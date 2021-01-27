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

package net.thesilkminer.mc.ematter.compatibility

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.bosonApi
import net.thesilkminer.mc.boson.api.compatibility.CompatibilityProvider
import net.thesilkminer.mc.boson.api.configuration.ConfigurationRegistry
import net.thesilkminer.mc.boson.api.event.CompatibilityProviderRegistryEvent

internal interface EnergyIsMatterCompatibilityProvider : CompatibilityProvider {
    fun onPreInitialization() {}
    fun onConfigurationRegistration(registry: ConfigurationRegistry) {}
}

internal object CompatibilityProviderHandler {
    @SubscribeEvent
    fun onCompatibilityProviderRegistration(e: CompatibilityProviderRegistryEvent) {
        e.registry.registerProvider(EnergyIsMatterCompatibilityProvider::class)
    }

    internal fun firePreInitializationEvent() {
        bosonApi.compatibilityProviderRegistry[EnergyIsMatterCompatibilityProvider::class].forEach(EnergyIsMatterCompatibilityProvider::onPreInitialization)
    }

    internal fun fireConfigurationRegistry(registry: ConfigurationRegistry) {
        bosonApi.compatibilityProviderRegistry[EnergyIsMatterCompatibilityProvider::class].forEach { it.onConfigurationRegistration(registry) }
    }
}
