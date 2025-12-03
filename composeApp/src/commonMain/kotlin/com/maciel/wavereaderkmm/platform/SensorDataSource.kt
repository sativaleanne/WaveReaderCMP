package com.maciel.wavereaderkmm.platform

/**
 * Sensor data from platform-specific sensors
 */
data class SensorData(
    val verticalAcceleration: Float,
    val horizontalX: Float,
    val horizontalY: Float,
    val samplingRate: Float
)

/**
 * Platform-specific sensor data source
 *
 * Android: Uses SensorManager
 * iOS: Uses CoreMotion
 */
expect class SensorDataSource {
    /**
     * Check if required sensors are available
     */
    fun areSensorsAvailable(): Boolean

    /**
     * Start listening to sensor data
     * @param onData Callback invoked when new sensor data is available
     */
    fun startListening(onData: (SensorData) -> Unit)

    /**
     * Stop listening to sensor data
     */
    fun stopListening()

    /**
     * Get current gyroscope-based direction
     * Returns null if gyroscope data isn't ready
     */
    fun getCurrentGyroDirection(): Float?
}