@file:JvmName("C")

package net.thesilkminer.mc.ematter.common

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.configuration.configuration
import net.thesilkminer.mc.boson.api.event.ConfigurationRegisterEvent
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.compatibility.CompatibilityProviderHandler

internal val commonConfiguration = configuration {
    owner = MOD_ID
    name = "common"
}

internal object ConfigurationRegistrationHandler {
    @SubscribeEvent
    fun onConfigurationRegistration(e: ConfigurationRegisterEvent) {
        e.configurationRegistry.registerConfiguration(commonConfiguration)
        // I don't particularly like this, but it is the most sensible thing to do, even if it is slightly reaching
        // across modules
        CompatibilityProviderHandler.fireConfigurationRegistry(e.configurationRegistry)
    }
}
