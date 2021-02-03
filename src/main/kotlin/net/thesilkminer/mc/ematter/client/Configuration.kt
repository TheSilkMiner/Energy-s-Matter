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
        "performance" {
            comment = "Manage disabling and enabling of certain features that may be taxing on performance"

            subCategories {
                "molecular_assembler_device" {
                    comment = "Manage the Molecular Assembler Device most performance intensive behavior"

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
