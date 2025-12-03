package com.maciel.wavereaderkmm.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Test function to verify FFT works correctly on both platforms.
 *
 * Creates a simple sine wave and verifies FFT produces expected peak frequency.
 */
fun testFFT(): Boolean {
    val n = 1024
    val samplingRate = 50f  // 50 Hz
    val testFrequency = 5f  // 5 Hz sine wave

    // Generate test signal: simple sine wave at 5 Hz
    val testSignal = List(n) { i ->
        cos(2 * PI * testFrequency * i / samplingRate).toFloat()
    }

    // Perform FFT
    val fftResult = getFft(testSignal, n)

    // Find peak frequency
    val peakIndex = getPeakIndex(fftResult, n)
    val peakFrequency = peakIndex * samplingRate / n

    // Verify peak is near expected frequency (5 Hz)
    val error = abs(peakFrequency - testFrequency)
    val success = error < 0.5f  // Allow 0.5 Hz error

    println("FFT Test Results:")
    println("  Expected frequency: $testFrequency Hz")
    println("  Detected frequency: $peakFrequency Hz")
    println("  Error: $error Hz")
    println("  Status: ${if (success) "✓ PASS" else "✗ FAIL"}")

    return success
}

/**
 * Visual comparison test - print some FFT values.
 */
fun debugFFT() {
    val testData = listOf(1f, 2f, 3f, 4f, 3f, 2f, 1f, 0f)
    val n = 8

    val result = getFft(testData, n)

    println("Input: $testData")
    println("FFT Output (first 8 complex values):")
    for (i in 0 until minOf(8, n)) {
        val real = result[2 * i]
        val imag = result[2 * i + 1]
        println("  [$i]: $real + ${imag}i")
    }
}