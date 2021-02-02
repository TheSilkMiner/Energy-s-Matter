@file:JvmName("BDSM")

package net.thesilkminer.mc.ematter.common.system

import net.minecraftforge.fml.common.eventhandler.EventBus
import net.thesilkminer.mc.ematter.common.system.temperature.condition.attachTemperatureTableConditionSerializersListener
import net.thesilkminer.mc.ematter.common.system.temperature.freezeTemperatureTables
import net.thesilkminer.mc.ematter.common.system.temperature.loadTemperatureTables

internal fun handleSystemsSetup(bus: EventBus) {
    attachTemperatureTableConditionSerializersListener(bus)
}

internal fun loadSystems() {
    loadTemperatureTables()
}

internal fun completeSystemLoading() {
    freezeTemperatureTables()
}
