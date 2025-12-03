package com.maciel.wavereaderkmm.platform


import androidx.compose.runtime.Composable

/**
 * Platform-specific dependency provider for sensor data
 *
 * Android: Uses SensorManager
 * iOS: Uses CoreMotion
 */
@Composable
expect fun rememberSensorDataSource(): SensorDataSource

@Composable
expect fun rememberLocationService(): LocationService