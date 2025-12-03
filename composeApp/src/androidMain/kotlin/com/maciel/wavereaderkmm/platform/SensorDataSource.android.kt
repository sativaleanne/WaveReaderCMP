package com.maciel.wavereaderkmm.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Android implementation using SensorManager
 */
actual class SensorDataSource(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private val alpha = 0.8f
    private val gravity = FloatArray(3)
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var filteredWaveDirection: Float? = null
    private var lastTimestamp: Long = 0L
    private var samplingRate: Float = 50f

    private var onDataCallback: ((SensorData) -> Unit)? = null

    actual fun areSensorsAvailable(): Boolean {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        return accelerometer != null && gyroscope != null && magnetometer != null
    }

    actual fun startListening(onData: (SensorData) -> Unit) {
        onDataCallback = onData

        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    actual fun stopListening() {
        sensorManager.unregisterListener(this)
        onDataCallback = null
    }

    actual fun getCurrentGyroDirection(): Float? {
        return filteredWaveDirection
    }

    override fun onSensorChanged(event: SensorEvent) {
        updateSamplingRate(event.timestamp)

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)

                val earthAcceleration = filterAccelerationData(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )

                // Send data to callback
                onDataCallback?.invoke(
                    SensorData(
                        verticalAcceleration = earthAcceleration[2],
                        horizontalX = earthAcceleration[0],
                        horizontalY = earthAcceleration[1],
                        samplingRate = samplingRate
                    )
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                val gyroscopeDt = if (lastTimestamp != 0L) {
                    (event.timestamp - lastTimestamp) / 1_000_000_000f
                } else 0f

                val gyroZ = event.values[2]
                val sensorHeading = getMagneticHeading()

                if (filteredWaveDirection == null) {
                    filteredWaveDirection = sensorHeading
                } else {
                    val integratedAngle = filteredWaveDirection!! + Math.toDegrees(gyroZ * gyroscopeDt.toDouble()).toFloat()
                    val blended = alpha * integratedAngle + (1 - alpha) * sensorHeading
                    filteredWaveDirection = (blended + 360) % 360
                }
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Remove gravity and transform to earth coordinates
     */
    private fun filterAccelerationData(x: Float, y: Float, z: Float): FloatArray {
        // Gravity filtering
        gravity[0] = alpha * gravity[0] + (1 - alpha) * x
        gravity[1] = alpha * gravity[1] + (1 - alpha) * y
        gravity[2] = alpha * gravity[2] + (1 - alpha) * z

        val linearAcc = floatArrayOf(
            x - gravity[0],
            y - gravity[1],
            z - gravity[2]
        )

        // Use rotation matrix to remap linear acceleration to earth coordinates
        val earthAcc = FloatArray(3)

        if (SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
            )
        ) {
            // Apply rotation matrix to get earth-relative acceleration
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                rotationMatrix
            )

            // Matrix multiplication: rotationMatrix * linearAcc
            earthAcc[0] = rotationMatrix[0] * linearAcc[0] + rotationMatrix[1] * linearAcc[1] + rotationMatrix[2] * linearAcc[2]
            earthAcc[1] = rotationMatrix[3] * linearAcc[0] + rotationMatrix[4] * linearAcc[1] + rotationMatrix[5] * linearAcc[2]
            earthAcc[2] = rotationMatrix[6] * linearAcc[0] + rotationMatrix[7] * linearAcc[1] + rotationMatrix[8] * linearAcc[2]
        } else {
            // Fallback if rotation matrix not available
            earthAcc[0] = linearAcc[0]
            earthAcc[1] = linearAcc[1]
            earthAcc[2] = linearAcc[2]
        }

        return earthAcc
    }

    /**
     * Get magnetic heading
     */
    private fun getMagneticHeading(): Float {
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        return (azimuth + 360) % 360
    }

    /**
     * Update sampling rate
     */
    private fun updateSamplingRate(currentTimestamp: Long) {
        if (lastTimestamp != 0L) {
            val dt = (currentTimestamp - lastTimestamp) / 1_000_000_000f
            samplingRate = if (dt > 0) 1f / dt else 0f
        }
        lastTimestamp = currentTimestamp
    }
}