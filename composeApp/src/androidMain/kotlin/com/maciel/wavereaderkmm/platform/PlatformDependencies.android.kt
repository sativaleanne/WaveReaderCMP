package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation - uses LocalContext to create SensorDataSource
 */
@Composable
actual fun rememberSensorDataSource(): SensorDataSource {
    val context = LocalContext.current
    return remember { SensorDataSource(context) }
}

/**
 * Android implementation - creates LocationService with Context
 */
@Composable
actual fun rememberLocationService(): LocationService {
    val context = LocalContext.current
    return remember { LocationService(context) }
}