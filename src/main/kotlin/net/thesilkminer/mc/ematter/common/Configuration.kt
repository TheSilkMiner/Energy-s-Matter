@file:JvmName("C")

package net.thesilkminer.mc.ematter.common

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.thesilkminer.mc.boson.api.configuration.ConfigurationFormat
import net.thesilkminer.mc.boson.api.configuration.EntryType
import net.thesilkminer.mc.boson.api.configuration.configuration
import net.thesilkminer.mc.boson.api.event.ConfigurationRegisterEvent
import net.thesilkminer.mc.ematter.MOD_ID
import net.thesilkminer.mc.ematter.compatibility.CompatibilityProviderHandler

internal val commonConfiguration = configuration {
    owner = MOD_ID
    name = "common"

    categories {
        "behavior" {
            comment = "Manage the behavior of certain items and tools"

            subCategories {
                "thermometer" {
                    comment = "Manage the Thermometer behavior"

                    entries {
                        "fluctuation"(EntryType.WHOLE_NUMBER) {
                            comment = "Indicates how much the temperature reading of the thermometer can fluctuate away from the real value"
                            default = 5
                            bounds(min = 0, max = 42)
                        }
                    }
                }
            }
        }
        "performance" {
            comment = "Manage disabling and enabling of certain features that may be taxing on performance"

            subCategories {
                "molecular_assembler_device" {
                    comment = "Manage the Molecular Assembler Device most performance intensive behavior"

                    entries {
                        "thread_the_hole"(EntryType.BOOLEAN) {
                            comment = "Whether the volume of the Molecular Assembler Device should accurately represent the block. This is VERY COSTLY ON PERFORMANCE!"
                            default = true
                            requiresGameRestart() // Not really, but we force it so that this cannot be abused
                        }
                    }
                }
            }
        }
    }
}

internal val kernelConfiguration = configuration {
    owner = MOD_ID
    name = "kernel"
    type = ConfigurationFormat.JSON

    categories {
        "scheduler" {
            comment = "Scheduler component"

            entries {
                "seebeck"(EntryType.LIST_OF_STRINGS) {
                    comment = "Manages the scheduling for the seebeck"
                    default = listOf("1.0", "60", "40")
                    requiresGameRestart()
                }
            }
        }
    }
}

internal object ConfigurationRegistrationHandler {
    @SubscribeEvent
    fun onConfigurationRegistration(e: ConfigurationRegisterEvent) {
        e.configurationRegistry.registerConfigurations(commonConfiguration, kernelConfiguration)
        // I don't particularly like this, but it is the most sensible thing to do, even if it is slightly reaching
        // across modules
        CompatibilityProviderHandler.fireConfigurationRegistry(e.configurationRegistry)
    }
}
