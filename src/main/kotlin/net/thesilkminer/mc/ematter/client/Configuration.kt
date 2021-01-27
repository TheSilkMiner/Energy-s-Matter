@file:JvmName("C")

package net.thesilkminer.mc.ematter.client

import net.thesilkminer.mc.boson.api.configuration.configuration
import net.thesilkminer.mc.boson.api.distribution.Distribution
import net.thesilkminer.mc.ematter.MOD_ID

internal val clientConfiguration = configuration {
    owner = MOD_ID
    name = "client"
    targetDistribution = Distribution.CLIENT
}
