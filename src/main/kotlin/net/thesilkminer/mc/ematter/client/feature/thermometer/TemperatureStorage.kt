package net.thesilkminer.mc.ematter.client.feature.thermometer

import kotlin.random.Random

internal object TemperatureStorage {
    private var internalTemperature: Int? = null

    val hasTemperature: Boolean get() = this.internalTemperature != null
    var dark: Boolean = false
        private set

    var temperature: Int
        get() = internalTemperature!!
        set(value) {
            internalTemperature = (value + Random.nextInt(from = -5, until = 6)) // TODO("Config check for accuracy")
            this.dark = Random.nextBoolean() // TODO("make this more meaningful")
        }

    fun unset() {
        this.internalTemperature = null
    }
}
