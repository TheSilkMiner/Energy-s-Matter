// this file is more or less a copy of Bosons power display utils. All credit goes to Silk
@file:JvmName("DU")

package net.thesilkminer.mc.ematter.compatibility.justenoughitems.ingredient.mole

import net.thesilkminer.mc.ematter.common.mole.Moles
import kotlin.math.roundToInt

const val MEASUREMENT_UNIT = "Mol"

fun Moles.toUserFriendlyAmount(decimalDigits: Int = -1): String {
    val roundingData = this.roundToSmallestDouble()
    return "${roundingData.first.truncateTo(if (roundingData.second == 0) 0 else decimalDigits)} ${roundingData.second.toUnitMultiplier()}$MEASUREMENT_UNIT"
}

fun Moles.toTargetUnit() = "${this.roundToSmallestDouble().second.toUnitMultiplier()}$MEASUREMENT_UNIT"

private fun Moles.roundToSmallestDouble(): Pair<Double, Int> {
    if (this == 0) return 0.0 to 0

    var doubleEquivalent = this.toDouble()
    var rounds = 0
    while ((doubleEquivalent / 1000.0).toInt() > 0) {
        doubleEquivalent /= 1000.0
        ++rounds
    }

    return doubleEquivalent to rounds
}

private fun Double.truncateTo(decimalDigits: Int) = when {
    decimalDigits < 0 -> this.toString()
    decimalDigits == 0 -> this.roundToInt().toString()
    else -> "%.${decimalDigits}f".format(this)
}

private fun Int.toUnitMultiplier() = when (this) {
    0 -> ""
    1 -> "k"
    2 -> "M"
    3 -> "G"
    else -> if (this < 0) throw IllegalStateException("Rounds was negative") else throw UnsupportedOperationException("$this is outside bounds for units")
}
