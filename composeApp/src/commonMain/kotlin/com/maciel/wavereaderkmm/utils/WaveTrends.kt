package com.maciel.wavereaderkmm.utils

import com.maciel.wavereaderkmm.model.MeasuredWaveData
import kotlin.math.pow
import kotlin.math.sqrt


/** Helper Function:
 * Returns the standard deviation (sample-based) of the list.
 * */
fun List<Float>.standardDeviation(): Float {
    if (size < 2) return 0f
    val mean = average().toFloat()
    val variance = this.map { (it - mean).pow(2) }.sum() / this.size
    return sqrt(variance)
}

/**
 * Gets z-score of the last value in the list.
 * */
fun List<Float>.zScore(): Float {
    if (size < 2) return 0f
    val mean = average().toFloat()
    val std = standardDeviation()
    return if (std == 0f) 0f else (last() - mean) / std
}

/**
 * Calculates moving average over a sliding window.
 * */
fun movingAverage(data: List<Float>, window: Int): List<Float> {
    if (data.size < window || window < 1) return emptyList()
    val averaged = data.windowed(window, step = 1) { it.average().toFloat() }

    // Pad to maintain original size
    val padding = window / 2
    val paddedStart = List(padding) { averaged.first() }
    val paddedEnd = List(padding) { averaged.last() }

    return paddedStart + averaged + paddedEnd
}

/** Median filter for spike rejection */
fun medianFilter(data: List<Float>, window: Int): List<Float> {
    if (data.size < window) return data
    val filtered = data.windowed(window, 1) { it.sorted()[window / 2] }

    // Pad to maintain original size
    val padding = window / 2
    val paddedStart = List(padding) { filtered.first() }
    val paddedEnd = List(padding) { filtered.last() }

    return paddedStart + filtered + paddedEnd
}

/** smooth output height/period*/
fun smoothOutput(previous: Float?, new: Float, alpha: Float = 0.9f): Float {
    return if (previous == null) new else alpha * previous + (1 - alpha) * new
}

/**
 * Linear regression slope
 * */
fun slope(data: List<Float>): Float {
    if (data.size < 2) return 0f
    val n = data.size
    val x = List(n) { it.toFloat() }
    val xMean = x.average().toFloat()
    val yMean = data.average().toFloat()

    val numerator = x.zip(data).map {
        (xi, yi) -> (xi - xMean) * (yi - yMean)
    }.sum()

    val denominator = x.map {
        xi -> (xi - xMean).pow(2)
    }.sum()

    return if (denominator == 0f) 0f else numerator / denominator
}

/**
 * Forecasts the next value in a list using slope.
 * */
fun forecastNext(data: List<Float>): Float? {
    if (data.size < 2) return null
    val m = slope(data)
    val xMean = data.indices.average().toFloat()
    val yMean = data.average().toFloat()
    val b = yMean - m * xMean
    return m * data.size + b
}

/**
 * Returns a confidence score that a big wave is coming up
 * based on recent rising slope and deviation
 */
fun nextBigWaveConfidence(waves: List<MeasuredWaveData>): Float {
    val recent = waves.takeLast(6)
    if (recent.size < 6) return 0f

    val heights = recent.map { it.waveHeight }

    // Height trend score
    val heightSlope = slope(heights).coerceAtLeast(0f)
    val trendScore = (heightSlope / 0.2f).coerceIn(0f, 1f)

    // Z-score
    val zScore = heights.zScore().coerceAtLeast(0f)
    val heightScore = (zScore / 2f).coerceIn(0f, 1f)

    // Magnitude confidence
    val magnitudeScore = (heights.last() / 3.0f).coerceIn(0f, 1f)

    // Weighted average
    return (0.4f * trendScore + 0.3f * heightScore + 0.3f * magnitudeScore).coerceIn(0f, 1f)
}

/**
 * Returns true if the confidence of a big wave is coming is high enough
 */
fun predictNextBigWave(waves: List<MeasuredWaveData>): Boolean {
    return nextBigWaveConfidence(waves) > 0.75f
}

