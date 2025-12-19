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
fun toRadians(deg: Double): Double = deg / 180.0 * PI
fun toRadians(deg: Float): Float = deg / 180f * PI.toFloat()
fun Float.toDegrees(): Float = this * 180f / PI.toFloat()
fun Double.toDegrees(): Double = this * 180.0 / PI

// Calculate significant wave height
fun calculateSignificantWaveHeight(m0: Float): Float {
    return 4 * sqrt(m0)
}

// Calculate mean wave period
fun calculateMeanPeriod(m0: Float, m1: Float): Float {
    // Tm01 = m0 / m1 (mean period)
    return if (m1 > 0f) m0 / m1 else 0f
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

// Calculate wave direction from cross-spectrum
fun calculateWaveDirection(accelX: List<Float>, accelY: List<Float>): Float {
    if (accelX.isEmpty() || accelY.isEmpty()) {
        println("Warning: data is empty!")
        return 0f
    }
    // Use a fixed window size for consistent FFT resolution
    val windowSize = 2048

    // Need enough data for meaningful direction estimate
    if (accelX.size < windowSize || accelY.size < windowSize) {
        return 0f  // Return 0° as default until enough data
    }

    // Take the most recent windowSize samples
    val recentX = accelX.takeLast(windowSize)
    val recentY = accelY.takeLast(windowSize)
    val n = windowSize

    val windowedX = hanningWindow(recentX)
    val windowedY = hanningWindow(recentY)

    val fftX = getFft(windowedX, n)
    val fftY = getFft(windowedY, n)

    // Find dominant frequency
    val peakFreq = getPeakIndex(fftX, n)

    // Get complex values at peak frequency
    val realX = fftX[2 * peakFreq]
    val imagX = fftX[2 * peakFreq + 1]
    val realY = fftY[2 * peakFreq]
    val imagY = fftY[2 * peakFreq + 1]

    // Direction from cross-spectrum
    var waveDirection = atan2(
        imagY * realX - realY * imagX,
        realY * realX + imagY * imagX
    ).toDegrees()

    if (waveDirection < 0) waveDirection += 360f

    return waveDirection
}

// Converts FFT output into a power spectrum (density per frequency band)
fun computeSpectralDensity(fft: FloatArray, n: Int, samplingRate: Float): List<Float> {
    val halfN = n / 2
    val dt = 1.0f / samplingRate  // ← ADD THIS

    return (0 until halfN).map { i ->
        val re = fft[2 * i]
        val im = fft[2 * i + 1]
        val magnitude = re * re + im * im
        // CORRECT: dividing by (n * Δt)
        magnitude / (n * dt)  // ← CHANGE THIS
    }
}

// Calculate spectral moments
fun calculateSpectralMoments(
    spectrum: List<Float>,
    samplingRate: Float,
    isAccelerationSpectrum: Boolean = true
): Triple<Float, Float, Float> {
    val df = samplingRate / (2 * spectrum.size)
    var m0 = 0f
    var m1 = 0f
    var m2 = 0f

    for (i in spectrum.indices) {
        val f = i * df
        if (f !in 0.05f..0.5f) continue

        var S = spectrum[i]

        // ← ADD THIS CONVERSION
        // Convert acceleration PSD to displacement PSD
        if (isAccelerationSpectrum && f > 0f) {
            val omega = 2.0f * PI.toFloat() * f  // Radian frequency
            S /= (omega * omega * omega * omega)  // Divide by ω⁴
        }

        m0 += S * df
        m1 += S * f * df
        m2 += S * f * f * df
    }

    return Triple(m0, m1, m2)
}

// Compute wave metrics from spectrum
fun computeWaveMetricsFromSpectrum(m0: Float, m1: Float, m2: Float): Triple<Float, Float, Float> {
    val significantHeight = calculateSignificantWaveHeight(m0)
    val meanPeriod = calculateMeanPeriod(m0, m1)
    val spectralZeroCrossingPeriod = if (m2 != 0f) sqrt(m0 / m2) else 0f
    return Triple(significantHeight, meanPeriod, spectralZeroCrossingPeriod)
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

    // Pad the rolling average to match original size
    val padding = windowSize / 2
    val paddedAvg = List(padding) { rollingAvg.first() } +
            rollingAvg +
            List(padding) { rollingAvg.last() }

    // Subtract from original data
    return data.mapIndexed { i, value ->
        value - paddedAvg.getOrElse(i) { 0f }
    }
}

fun testDegreesConversion() {
    val radians = PI.toFloat()  // 180 degrees
    val degrees = radians.toDegrees()
    println("π radians = $degrees degrees")
    println("Test ${if (degrees == 180f) "PASSED ✓" else "FAILED ✗"}")
}