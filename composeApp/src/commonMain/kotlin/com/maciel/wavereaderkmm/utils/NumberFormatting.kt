package com.maciel.wavereaderkmm.utils

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Multiplatform number formatting utilities
 */

/**
 * Format a Double to a string with specified decimal places
 */
fun Double.toDecimalString(decimalPlaces: Int): String {
    if (this.isNaN() || this.isInfinite()) return this.toString()

    val multiplier = 10.0.pow(decimalPlaces)
    val rounded = (this * multiplier).roundToInt() / multiplier

    // Build the string manually
    val intPart = rounded.toInt()
    val decimalPart = abs((rounded - intPart) * multiplier).roundToInt()

    return if (decimalPlaces > 0) {
        val decimalStr = decimalPart.toString().padStart(decimalPlaces, '0')
        "$intPart.$decimalStr"
    } else {
        intPart.toString()
    }
}

/**
 * Format a Float to a string with specified decimal places
 */
fun Float.toDecimalString(decimalPlaces: Int): String {
    return this.toDouble().toDecimalString(decimalPlaces)
}

/**
 * Format latitude/longitude coordinates
 */
fun formatLatLong(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    val latAbs = abs(lat).toDecimalString(4)
    val lonAbs = abs(lon).toDecimalString(4)

    return "${latAbs}°$latDir, ${lonAbs}°$lonDir"
}