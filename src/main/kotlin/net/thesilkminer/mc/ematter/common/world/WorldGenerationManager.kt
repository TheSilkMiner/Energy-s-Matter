@file:JvmName("WGM")

package net.thesilkminer.mc.ematter.common.world

import net.thesilkminer.mc.ematter.common.world.ore.registerOreGenerationManager

internal fun registerWorldGenerationProviders() {
    registerOreGenerationManager()
}
