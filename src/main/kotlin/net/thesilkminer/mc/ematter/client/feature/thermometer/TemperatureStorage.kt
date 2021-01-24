package net.thesilkminer.mc.ematter.client.feature.thermometer

internal object TemperatureStorage {
    private var internalTemperature: Int? = null

    val hasTemperature: Boolean get() = this.internalTemperature != null

    var temperature: Int
        get() = internalTemperature!!
        set(value) { internalTemperature = value }

    fun unset() {
        this.internalTemperature = null
    }
}
