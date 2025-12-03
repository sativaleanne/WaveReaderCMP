package com.maciel.wavereaderkmm.utils

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Wave Calculations
 */

// Extension functions for radians to degrees conversion
// These replace Java's Math.toDegrees()
fun Float.toDegrees(): Float = this * 180f / PI.toFloat()
fun Double.toDegrees(): Double = this * 180.0 / PI

// Calculate significant wave height
fun calculateSignificantWaveHeight(m0: Float): Float {
    return 4 * sqrt(m0)
}

// Calculate average wave period
fun calculateAveragePeriod(m0: Float, m1: Float): Float {
    return if (m1 != 0f) m0 / m1 else 0f
}

// Hanning window to reduce spectral leaks
fun hanningWindow(data: List<Float>): List<Float> {
    val n = data.size
    return data.mapIndexed { i, value ->
        val multiplier = 0.5f * (1 - cos(2 * PI * i / (n - 1))).toFloat()
        value * multiplier
    }
}

// Get Index of max magnitude in FFT output
fun getPeakIndex(data: FloatArray, n: Int): Int {
    var maxMagnitude = 0f
    var peakIndex = 0

    for (i in 1 until n / 2) {
        val real = data[2 * i]
        val imaginary = data[2 * i + 1]
        val magnitude = sqrt(real.pow(2) + imaginary.pow(2))

        if (magnitude > maxMagnitude) {
            maxMagnitude = magnitude
            peakIndex = i
        }
    }
    return peakIndex
}

// Calculate wave direction using phase difference
fun calculateWaveDirection(accelX: List<Float>, accelY: List<Float>): Float {
    if (accelX.isEmpty() || accelY.isEmpty()) {
        println("Warning: data is empty!")
        return 0f
    }
    val n = accelX.size

    val windowedX = hanningWindow(accelX)
    val windowedY = hanningWindow(accelY)

    val fftX = getFft(windowedX, n)
    val fftY = getFft(windowedY, n)

    val peakX = getPeakIndex(fftX, n)
    val peakY = getPeakIndex(fftY, n)

    // Get phase angles at the dominant frequency
    val phaseX = atan2(fftX[2 * peakX + 1], fftX[2 * peakX])
    val phaseY = atan2(fftY[2 * peakY + 1], fftY[2 * peakY])

    val phaseDifference = phaseY - phaseX

    // Compute wave direction in degrees
    var waveDirection = phaseDifference.toDegrees()
    if (waveDirection < 0) waveDirection += 360f // Normalize to 0-360°

    return waveDirection
}

// Converts FFT output into a power spectrum (density per frequency band)
fun computeSpectralDensity(fft: FloatArray, n: Int): List<Float> {
    val halfN = n / 2
    return (0 until halfN).map { i ->
        val re = fft[2 * i]
        val im = fft[2 * i + 1]
        (re * re + im * im) / n
    }
}

// Calculate spectral moments
fun calculateSpectralMoments(spectrum: List<Float>, samplingRate: Float): Triple<Float, Float, Float> {
    val df = samplingRate / (2 * spectrum.size) // frequency resolution
    val freq = spectrum.indices.map { it * df }

    var m0 = 0f
    var m1 = 0f
    var m2 = 0f

    for (i in spectrum.indices) {
        val S = spectrum[i]
        val f = freq[i]
        m0 += S * df
        m1 += S * f * df
        m2 += S * f * f * df
    }

    return Triple(m0, m1, m2)
}

// Compute wave metrics from spectrum
fun computeWaveMetricsFromSpectrum(m0: Float, m1: Float, m2: Float): Triple<Float, Float, Float> {
    val significantHeight = calculateSignificantWaveHeight(m0)
    val avgPeriod = calculateAveragePeriod(m0, m1)
    val zeroCrossingPeriod = if (m2 != 0f) sqrt(m0 / m2) else 0f
    return Triple(significantHeight, avgPeriod, zeroCrossingPeriod)
}

// Estimate zero-crossing period
fun estimateZeroCrossingPeriod(signal: List<Float>, samplingRate: Float): Float {
    val zeroCrossings = mutableListOf<Int>()

    for (i in 1 until signal.size) {
        val prev = signal[i - 1]
        val curr = signal[i]
        if (prev < 0 && curr >= 0) {  // upward zero-crossing
            zeroCrossings.add(i)
        }
    }

    if (zeroCrossings.size < 2) return Float.NaN

    val intervals = zeroCrossings.zipWithNext { a, b -> b - a }
    val averageSamples = intervals.average().toFloat()
    return averageSamples / samplingRate  // period in seconds
}

// High-pass filter to remove drift
fun highPassFilter(data: List<Float>, windowSize: Int): List<Float> {
    if (data.size < windowSize) return data
    val rollingAvg = data.windowed(windowSize, 1) { it.average().toFloat() }
    return data.drop(windowSize - 1).mapIndexed { i, value ->
        val avg = rollingAvg.getOrElse(i) { 0f }
        value - avg
    }
}

fun testDegreesConversion() {
    val radians = PI.toFloat()  // 180 degrees
    val degrees = radians.toDegrees()
    println("π radians = $degrees degrees")
    println("Test ${if (degrees == 180f) "PASSED ✓" else "FAILED ✗"}")
}