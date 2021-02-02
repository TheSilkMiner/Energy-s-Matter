@file:JvmName("JEIC")

package net.thesilkminer.mc.ematter.compatibility.justenoughitems

import net.thesilkminer.mc.boson.api.configuration.EntryType
import net.thesilkminer.mc.boson.api.configuration.configuration
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.ematter.MOD_ID

internal val justEnoughItemsConfiguration = configuration {
    owner = MOD_ID
    name = "compatibility/just_enough_items"
    targetDistribution = Distribution.CLIENT

    categories {
        "recipes" {
            comment = "Manage the recipe aspect of the JustEnoughItems integration for Energy's Matter"

            subCategories {
                "molecular_assembler_device" {
                    comment = "Manage the Molecular Assembler Device display of certain recipes"

                    entries {
                        "missing_on_crafting"(EntryType.BOOLEAN) {
                            comment = "Show a Missing Data entry for recipes that can be crafted without a Molecular Assembler Device"
                            default = true
                        }
                    }
                }
            }
        }
    }
}
