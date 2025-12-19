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
    val dt = 1.0f / samplingRate

    return (0 until halfN).map { i ->
        val re = fft[2 * i]
        val im = fft[2 * i + 1]
        val magnitude = re * re + im * im
        // dividing by (n * Δt)
        magnitude / (n * dt)
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
fun estimateZeroCrossingPeriod(
    accelerationSignal: List<Float>,
    samplingRate: Float
): Float {
    if (accelerationSignal.size < 128) {
        return Float.NaN  // Need sufficient data
    }

    // Step 1: Prepare data - use power-of-2 size
    val n = accelerationSignal.size.takeHighestOneBit()  // Nearest power of 2
    val signal = accelerationSignal.take(n)

    // Step 2: Apply Hanning window to reduce spectral leakage
    val windowed = hanningWindow(signal)

    // Step 3: FFT to frequency domain
    val fft = getFft(windowed, n)
    val df = samplingRate / n

    // Step 4: Integrate twice in frequency domain (acceleration → displacement)
    // Division by (2πf)² for each integration
    val displacementFFT = FloatArray(fft.size)

    for (i in 1 until n/2) {  // Skip DC component (i=0)
        val f = i * df

        // Only process wave frequencies (0.05 - 0.5 Hz)
        // Below 0.05 Hz: avoid division by very small numbers
        // Above 0.5 Hz: typically noise for ocean waves
        if (f !in 0.05f..0.5f) {
            displacementFFT[2*i] = 0f
            displacementFFT[2*i + 1] = 0f
            continue
        }

        val omega = 2.0f * PI.toFloat() * f
        val integrationFactor = 1.0f / (omega * omega)  // Divide by ω² for acceleration→displacement

        // Apply integration to both real and imaginary parts
        displacementFFT[2*i] = fft[2*i] * integrationFactor
        displacementFFT[2*i + 1] = fft[2*i + 1] * integrationFactor

        // Mirror for negative frequencies (Hermitian symmetry for real signal)
        if (i < n/2) {
            displacementFFT[2*(n-i)] = displacementFFT[2*i]
            displacementFFT[2*(n-i) + 1] = -displacementFFT[2*i + 1]
        }
    }

    // Step 5: Inverse FFT to get displacement in time domain
    val displacement = getIfft(displacementFFT, n)

    // Step 6: Remove mean (detrend)
    val mean = displacement.average().toFloat()
    val detrendedDisplacement = displacement.map { it - mean }

    // Step 7: Find upward zero-crossings (negative to positive)
    val zeroCrossings = mutableListOf<Int>()
    for (i in 1 until detrendedDisplacement.size) {
        val prev = detrendedDisplacement[i - 1]
        val curr = detrendedDisplacement[i]

        // Upward zero-crossing
        if (prev < 0 && curr >= 0) {
            zeroCrossings.add(i)
        }
    }

    // Step 8: Calculate average period from zero-crossing intervals
    if (zeroCrossings.size < 2) {
        return Float.NaN  // Need at least 2 crossings
    }

    // Calculate intervals between consecutive zero-crossings
    val intervals = zeroCrossings.zipWithNext { a, b -> b - a }

    // Remove outliers (intervals outside 2-25 seconds for realistic ocean waves)
    val minSamples = (2.0f * samplingRate).toInt()  // 2 seconds
    val maxSamples = (25.0f * samplingRate).toInt()  // 25 seconds
    val validIntervals = intervals.filter { it in minSamples..maxSamples }

    if (validIntervals.isEmpty()) {
        return Float.NaN
    }

    // Average interval in samples, convert to seconds
    val averageSamples = validIntervals.average().toFloat()
    return averageSamples / samplingRate
}

/**
 * Helper extension function to get the highest one-bit (nearest power of 2, rounded down)
 */
private fun Int.takeHighestOneBit(): Int {
    var n = this
    n = n or (n shr 1)
    n = n or (n shr 2)
    n = n or (n shr 4)
    n = n or (n shr 8)
    n = n or (n shr 16)
    return n - (n shr 1)
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