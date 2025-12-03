package com.maciel.wavereaderkmm.utils

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

/**
 * Pure Kotlin FFT Implementation
 * Cooley-Tukey algorithm
 *
 * - Input size must be power of 2 (1024, 2048, etc.)
 */

data class Complex(val real: Double, val imag: Double) {
    operator fun plus(other: Complex) = Complex(real + other.real, imag + other.imag)
    operator fun minus(other: Complex) = Complex(real - other.real, imag - other.imag)
    operator fun times(other: Complex) = Complex(
        real * other.real - imag * other.imag,
        real * other.imag + imag * other.real
    )

    fun toFloat() = real.toFloat()
}

/**
 * Cooley-Tukey FFT algorithm
 * Radix-2 decimation-in-time
 */
private fun fft(x: Array<Complex>): Array<Complex> {
    val n = x.size

    // Base case
    if (n == 1) return x

    // Check if power of 2
    if (n and (n - 1) != 0) {
        throw IllegalArgumentException("FFT size must be power of 2, got $n")
    }

    // Divide into even and odd
    val even = Array(n / 2) { x[2 * it] }
    val odd = Array(n / 2) { x[2 * it + 1] }

    // Conquer: recursively compute FFT of even and odd parts
    val evenFFT = fft(even)
    val oddFFT = fft(odd)

    // Combine
    val result = Array(n) { Complex(0.0, 0.0) }
    for (k in 0 until n / 2) {
        val angle = -2.0 * PI * k / n
        val w = Complex(cos(angle), sin(angle))
        val t = w * oddFFT[k]

        result[k] = evenFFT[k] + t
        result[k + n / 2] = evenFFT[k] - t
    }

    return result
}

/**
 * Performs FFT on real-valued input.
 * Returns interleaved complex output: [real0, imag0, real1, imag1, ...]
 */
fun getFft(data: List<Float>, n: Int): FloatArray {
    // Convert input to Complex array
    val input = Array(n) { i ->
        if (i < data.size) {
            Complex(data[i].toDouble(), 0.0)
        } else {
            Complex(0.0, 0.0)
        }
    }

    // Perform FFT
    val output = fft(input)

    // Convert to interleaved format [real0, imag0, real1, imag1, ...]
    val result = FloatArray(2 * n)
    for (i in 0 until n) {
        result[2 * i] = output[i].real.toFloat()
        result[2 * i + 1] = output[i].imag.toFloat()
    }

    return result
}

/**
 * Optimized bit-reversal for in-place FFT
 */
private fun bitReverse(n: Int): IntArray {
    val bits = (ln(n.toDouble()) / ln(2.0)).toInt()
    val reversed = IntArray(n)

    for (i in 0 until n) {
        var rev = 0
        var num = i
        for (j in 0 until bits) {
            rev = (rev shl 1) or (num and 1)
            num = num shr 1
        }
        reversed[i] = rev
    }

    return reversed
}

/**
 * Iterative FFT (more efficient for large sizes)
 * Optional optimization if recursive version is too slow
 */
fun fftIterative(data: List<Float>, n: Int): FloatArray {
    // Convert to complex
    val x = Array(n) { i ->
        if (i < data.size) {
            Complex(data[i].toDouble(), 0.0)
        } else {
            Complex(0.0, 0.0)
        }
    }

    // Bit-reversal permutation
    val reversed = bitReverse(n)
    val y = Array(n) { x[reversed[it]] }

    // Iterative FFT
    var size = 2
    while (size <= n) {
        val halfSize = size / 2
        val angle = -2.0 * PI / size

        for (i in 0 until n step size) {
            for (j in 0 until halfSize) {
                val w = Complex(cos(angle * j), sin(angle * j))
                val t = w * y[i + j + halfSize]
                val u = y[i + j]

                y[i + j] = u + t
                y[i + j + halfSize] = u - t
            }
        }

        size *= 2
    }

    // Convert to output format
    val result = FloatArray(2 * n)
    for (i in 0 until n) {
        result[2 * i] = y[i].real.toFloat()
        result[2 * i + 1] = y[i].imag.toFloat()
    }

    return result
}