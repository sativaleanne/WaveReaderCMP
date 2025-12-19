package com.maciel.wavereaderkmm.processing

import com.maciel.wavereaderkmm.utils.calculateSpectralMoments
import com.maciel.wavereaderkmm.utils.calculateWaveDirection
import com.maciel.wavereaderkmm.utils.computeSpectralDensity
import com.maciel.wavereaderkmm.utils.computeWaveMetricsFromSpectrum
import com.maciel.wavereaderkmm.utils.estimateZeroCrossingPeriod
import com.maciel.wavereaderkmm.utils.getFft
import com.maciel.wavereaderkmm.utils.hanningWindow
import com.maciel.wavereaderkmm.utils.highPassFilter
import com.maciel.wavereaderkmm.utils.medianFilter
import com.maciel.wavereaderkmm.utils.movingAverage
import com.maciel.wavereaderkmm.utils.smoothOutput
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Platform-agnostic wave data processor
 * Handles all the FFT and wave metric calculations
 */
class WaveDataProcessor {
    private val verticalAcceleration = mutableListOf<Float>()
    private val horizontalAcceleration = mutableListOf<Pair<Float, Float>>()

    private var previousFilteredDirection: Float? = null
    private var samplingRate: Float = 50f

    /**
     * Add new acceleration data point
     */
    fun addAccelerationData(
        vertical: Float,
        horizontalX: Float,
        horizontalY: Float
    ) {
        verticalAcceleration.add(vertical)
        horizontalAcceleration.add(Pair(horizontalX, horizontalY))

        // Trim buffer size
        if (verticalAcceleration.size > 2048) verticalAcceleration.removeAt(0)
        if (horizontalAcceleration.size > 2048) horizontalAcceleration.removeAt(0)
    }

    /**
     * Update sampling rate
     */
    fun updateSamplingRate(rate: Float) {
        samplingRate = rate
    }

    /**
     * Process accumulated data and calculate wave metrics
     *
     * @param gyroDirection Current gyroscope-based direction
     * @return Triple of (height, period, direction) or null if not enough data
     */
    fun processWaveData(gyroDirection: Float?): Triple<Float, Float, Float>? {
        if (verticalAcceleration.size < 1024) {
            return null
            //return Triple(0f, 0f, 0f)
        }

        if (!isActualMotion(verticalAcceleration)) {
            println("Phone appears stationary - no wave processing")
            return Triple(0f, 0f, 0f)
        }

        val windowSize = 1024
        val stepSize = 512

        // High-pass filter removes tilt and drift
        val highPassed = highPassFilter(verticalAcceleration, 51)

        // Segment data into overlapping windows
        val segments = overlapData(highPassed, windowSize, stepSize)

        val heights = mutableListOf<Float>()
        val periods = mutableListOf<Float>()

        for (segment in segments) {
            // Reject extremely high or low windows
            val rms = sqrt(segment.sumOf { it.toDouble() * it.toDouble() } / segment.size)
            if (rms < 0.01f) {
                println("Segment RMS too low: $rms m/s²")
                continue
            }

            if (rms > 10f) {
                println("Segment RMS too high: $rms m/s² (likely invalid)")
                continue
            }

            // Median filter to remove noise
            val medianed = medianFilter(segment, 5)

            val smoothed = movingAverage(medianed, 5)

            val windowed = hanningWindow(smoothed)

            // Compute frequency-domain features
            val fft = getFft(windowed, windowed.size)
            val spectrum = computeSpectralDensity(fft, windowed.size, samplingRate)
            val (m0, m1, m2) = calculateSpectralMoments(spectrum, samplingRate, isAccelerationSpectrum = true)

            if (m0 < 0.00003f) {
                println("Spectral energy too low: m0=$m0")
                continue
            }

            // Get spectral and zero-crossing wave periods
            val (sigWaveHeight, avePeriod, spectralZeroCrossPeriod) = computeWaveMetricsFromSpectrum(m0, m1, m2)
            val measuredZeroCrossPeriod = estimateZeroCrossingPeriod(segment, samplingRate)

            val finalPeriod = if (measuredZeroCrossPeriod.isFinite() &&
                measuredZeroCrossPeriod in 2f..25f) {  // Realistic range
                val relativeDiff = abs(spectralZeroCrossPeriod - measuredZeroCrossPeriod) /
                        spectralZeroCrossPeriod

                when {
                    relativeDiff < 0.15f -> spectralZeroCrossPeriod
                    relativeDiff < 0.30f -> 0.7f * spectralZeroCrossPeriod +
                            0.3f * measuredZeroCrossPeriod
                    else -> {
                        println("Large disagreement - using spectral")
                        spectralZeroCrossPeriod
                    }
                }
            } else {
                spectralZeroCrossPeriod
            }


            // Only valid stuff
            if (sigWaveHeight.isFinite() &&
                finalPeriod.isFinite() &&
                sigWaveHeight > 0.15f) {  // Minimum
                println("Valid wave detected: height=$sigWaveHeight, period=$finalPeriod")

                heights.add(sigWaveHeight)
                periods.add(finalPeriod)
            }
        }

        if (heights.isEmpty() || periods.isEmpty()) {
            println("No valid wave segments detected - returning zeros")
            return Triple(0f, 0f, 0f)
        }

        // Smoothing
        val avgHeight = heights.average().toFloat()
        val avgPeriod = periods.average().toFloat()

        // Calculate wave direction using horizontal motion
        val accelX = horizontalAcceleration.map { it.first }
        val accelY = horizontalAcceleration.map { it.second }
        val rawFftDirection = calculateWaveDirection(accelX, accelY)

        val fftDirection = previousFilteredDirection?.let {
            val delta = abs(rawFftDirection - it)
            if (delta < 45f) smoothOutput(it, rawFftDirection, alpha = 0.8f) else it
        } ?: rawFftDirection
        previousFilteredDirection = fftDirection

        // Combine with gyroscope direction if available
        val direction = gyroDirection?.let {
            (it + fftDirection) / 2f
        } ?: fftDirection

        return Triple(avgHeight, avgPeriod, direction)
    }

    /**
     * Clear all accumulated data
     */
    fun clear() {
        verticalAcceleration.clear()
        horizontalAcceleration.clear()
        previousFilteredDirection = null
    }

    /**
     * Divide data into overlapping chunks
     */
    private fun overlapData(data: List<Float>, windowSize: Int, stepSize: Int): List<List<Float>> {
        val segments = mutableListOf<List<Float>>()
        var i = 0
        while (i + windowSize <= data.size) {
            segments.add(data.subList(i, i + windowSize))
            i += stepSize
        }
        return segments
    }

    /**
     * Check if there's actual wave motion vs just sensor noise
     * Call this BEFORE processing segments
     */
    fun isActualMotion(verticalAcceleration: List<Float>): Boolean {
        if (verticalAcceleration.size < 100) return false

        // 1. Calculate standard deviation
        val mean = verticalAcceleration.average().toFloat()
        val variance = verticalAcceleration.sumOf { (it - mean).toDouble().pow(2.0) } / verticalAcceleration.size
        val stdDev = sqrt(variance).toFloat()

        // 2. Motion threshold: 3x typical sensor noise
        // Typical phone noise: ~0.015 m/s²
        // Motion threshold: ~0.05 m/s²
        val motionThreshold = 0.05f

        if (stdDev < motionThreshold) {
            println("No significant motion detected: stdDev=$stdDev m/s² (threshold=$motionThreshold)")
            return false
        }

        // 3. Check for periodic motion (not just random spikes)
        val highPassed = highPassFilter(verticalAcceleration, 51)
        val acMean = highPassed.average().toFloat()
        val acVariance = highPassed.sumOf { (it - acMean).toDouble().pow(2.0) } / highPassed.size
        val acStdDev = sqrt(acVariance).toFloat()

        // After high-pass, should still have significant energy
        if (acStdDev < motionThreshold * 0.6f) {
            println("No periodic motion after filtering: stdDev=$acStdDev m/s²")
            return false
        }

        println("✓ Motion detected: stdDev=$stdDev m/s²")
        return true
    }
}