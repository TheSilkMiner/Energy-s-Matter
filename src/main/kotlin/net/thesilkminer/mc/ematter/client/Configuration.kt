@file:JvmName("C")

package net.thesilkminer.mc.ematter.client

import net.thesilkminer.mc.boson.api.configuration.EntryType
import net.thesilkminer.mc.boson.api.configuration.configuration
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.ematter.MOD_ID

internal val clientConfiguration = configuration {
    owner = MOD_ID
    name = "client"
    targetDistribution = Distribution.CLIENT

    categories {
        "behavior" {
            comment = "Manage the behavior of certain items and tools"

            subCategories {
                "molecular_assembler_device" {
                    comment = "Manage the Molecular Assembler Device behavior"

                    entries {
                        "spin_meaning"(EntryType.WHOLE_NUMBER) {
                            comment = "Set the meaning of the spinning item in the Molecular Assembler Device: 0 = none, 1 = charge level, 2 = recipe cost"
                            default = 1
                            bounds(min = 0, max = 2)
                            requiresGameRestart()
                        }
                    }
                }
            }
        }
        "performance" {
            comment = "Manage disabling and enabling of certain features that may be taxing on performance"

            subCategories {
                "molecular_assembler_device" {
                    comment = "Manage the Molecular Assembler Device most performance intensive components"

                    entries {
                        "force_full_highlight"(EntryType.BOOLEAN) {
                            comment = "Force the highlight of the Molecular Assembler Device to correctly represent the block, even if it doesn't match the actual volume"
                        }
                    }
                }
            }
        }
    }
}
