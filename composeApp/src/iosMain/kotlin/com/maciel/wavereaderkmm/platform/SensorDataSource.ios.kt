package com.maciel.wavereaderkmm.platform

import platform.CoreMotion.*
import platform.Foundation.NSOperationQueue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

/**
 * iOS implementation using CoreMotion framework
 *
 * Matches the actual SensorData structure:
 * - verticalAcceleration: Z-axis (up/down motion)
 * - horizontalX: X-axis (side-to-side)
 * - horizontalY: Y-axis (forward-back)
 * - samplingRate: Current sampling frequency
 */
@OptIn(ExperimentalForeignApi::class)
actual class SensorDataSource {

    private val motionManager = CMMotionManager()
    private val operationQueue = NSOperationQueue.currentQueue() ?: NSOperationQueue()

    private var isListening = false
    private var currentCallback: ((SensorData) -> Unit)? = null

    // Track timing for sampling rate calculation
    private var lastTimestamp: Double = 0.0

    /**
     * Check if required sensors are available
     */
    actual fun areSensorsAvailable(): Boolean {
        return motionManager.isDeviceMotionAvailable()
    }

    /**
     * Start listening to sensor data
     * Uses CMDeviceMotion for combined, filtered sensor data
     *
     * @param onData Callback invoked with sensor data at ~50Hz
     */
    actual fun startListening(onData: (SensorData) -> Unit) {
        if (isListening) {
            stopListening()
        }

        currentCallback = onData
        isListening = true
        lastTimestamp = 0.0

        // Set update interval (50Hz = 0.02 seconds)
        motionManager.deviceMotionUpdateInterval = 0.02

        // Start device motion updates (combines all sensors)
        motionManager.startDeviceMotionUpdatesToQueue(
            queue = operationQueue
        ) { motion, error ->
            if (error == null && motion != null) {
                handleDeviceMotionUpdate(motion)
            }
        }
    }

    /**
     * Stop listening to sensor data
     */
    actual fun stopListening() {
        if (!isListening) return

        motionManager.stopDeviceMotionUpdates()
        isListening = false
        currentCallback = null
        lastTimestamp = 0.0
    }

    /**
     * Get current gyroscope direction (heading)
     * Returns current yaw (rotation around vertical axis) in degrees
     */
    actual fun getCurrentGyroDirection(): Float? {
        val motion = motionManager.deviceMotion ?: return null

        // Get yaw from attitude (rotation around Z-axis)
        val yawRadians = motion.attitude.yaw

        // Convert radians to degrees manually
        val degrees = (yawRadians * 180.0 / kotlin.math.PI).toFloat()

        // Normalize to 0-360
        return (degrees + 360f) % 360f
    }

    /**
     * Handle device motion update
     *
     * Extracts only the data needed for wave measurement:
     * - Vertical acceleration (Z-axis) - primary wave motion
     * - Horizontal X and Y - for wave direction calculation
     * - Sampling rate - for FFT analysis
     */
    private fun handleDeviceMotionUpdate(motion: CMDeviceMotion) {
        // Calculate sampling rate from timestamp
        val currentTimestamp = motion.timestamp
        val samplingRate = if (lastTimestamp > 0.0) {
            val dt = currentTimestamp - lastTimestamp
            if (dt > 0) (1.0 / dt).toFloat() else 50f
        } else {
            50f // Default to 50Hz for first sample
        }
        lastTimestamp = currentTimestamp

        // Extract user acceleration (gravity already removed by iOS!)
        // userAcceleration is a CMAcceleration struct - need useContents

        // Vertical acceleration (Z-axis) - up/down motion
        val verticalAccel = motion.userAcceleration.useContents {
            (z * 9.81).toFloat()  // Convert g to m/s²
        }

        // Horizontal X (side-to-side when phone upright)
        val horizontalX = motion.userAcceleration.useContents {
            (x * 9.81).toFloat()
        }

        // Horizontal Y (forward-back when phone upright)
        val horizontalY = motion.userAcceleration.useContents {
            (y * 9.81).toFloat()
        }

        val sensorData = SensorData(
            verticalAcceleration = verticalAccel,
            horizontalX = horizontalX,
            horizontalY = horizontalY,
            samplingRate = samplingRate
        )

        currentCallback?.invoke(sensorData)
    }
}