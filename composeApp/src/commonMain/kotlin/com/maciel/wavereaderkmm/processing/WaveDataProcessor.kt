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
        if (verticalAcceleration.size < 1024) return null

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
            if (rms < 0.01f || rms > 10f) continue

            // Median filter to remove noise
            val medianed = medianFilter(segment, 5)

            // Smoothing
            val smoothed = movingAverage(medianed, 5)

            // Windowing
            val windowed = hanningWindow(smoothed)

            // Compute frequency-domain features
            val fft = getFft(windowed, windowed.size)
            val spectrum = computeSpectralDensity(fft, windowed.size)
            val (m0, m1, m2) = calculateSpectralMoments(spectrum, samplingRate)

            // Get spectral and zero-crossing wave periods
            val (sigWaveHeight, avePeriod, spectralZeroCrossPeriod) = computeWaveMetricsFromSpectrum(m0, m1, m2)
            val measuredZeroCrossPeriod = estimateZeroCrossingPeriod(segment, samplingRate)

            // Stability
//            val finalPeriod = if (measuredZeroCrossPeriod.isFinite()) {
//                (spectralZeroCrossPeriod + measuredZeroCrossPeriod) / 2f
//            } else {
//                spectralZeroCrossPeriod
//            }
            val finalPeriod = spectralZeroCrossPeriod

            // Only valid stuff
            if (sigWaveHeight.isFinite() && finalPeriod.isFinite()) {
                heights.add(sigWaveHeight)
                periods.add(finalPeriod)
            }
        }

        if (heights.isEmpty() || periods.isEmpty()) return null

        // Smoothing
        val avgHeight = heights.average().toFloat()
        val avgPeriod = periods.average().toFloat()

        // Calculate wave direction using horizontal motion
        val accelX = horizontalAcceleration.map { it.first }
        val accelY = horizontalAcceleration.map { it.second }
        val rawFftDirection = calculateWaveDirection(accelX, accelY)

        val fftDirection = previousFilteredDirection?.let {
            val delta = kotlin.math.abs(rawFftDirection - it)
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
}