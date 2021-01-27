@file:JvmName("JEIC")

package net.thesilkminer.mc.ematter.compatibility.justenoughitems

import net.thesilkminer.mc.boson.api.configuration.configuration
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.ematter.MOD_ID

internal val justEnoughItemsConfiguration = configuration {
    owner = MOD_ID
    name = "compatibility/just_enough_items"
    targetDistribution = Distribution.CLIENT
}
