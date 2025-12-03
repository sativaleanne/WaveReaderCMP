package com.maciel.wavereaderkmm.platform

// Stub for now - implement later with CoreMotion
actual class SensorDataSource {

    actual fun areSensorsAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun startListening(onData: (SensorData) -> Unit) {
    }

    actual fun stopListening() {
    }

    actual fun getCurrentGyroDirection(): Float? {
        TODO("Not yet implemented")
    }
}