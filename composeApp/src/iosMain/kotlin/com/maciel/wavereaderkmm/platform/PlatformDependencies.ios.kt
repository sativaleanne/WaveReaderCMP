package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation - creates iOS-specific SensorDataSource
 */
@Composable
actual fun rememberSensorDataSource(): SensorDataSource {
    return remember { SensorDataSource() }
}

/**
 * iOS implementation - creates LocationService (no context needed)
 */
@Composable
actual fun rememberLocationService(): LocationService {
    return remember { LocationService() }
}