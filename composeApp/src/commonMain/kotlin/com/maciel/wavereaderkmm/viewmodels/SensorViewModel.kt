package com.maciel.wavereaderkmm.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciel.wavereaderkmm.data.FirestoreRepository
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.platform.AppLogger
import com.maciel.wavereaderkmm.platform.SensorDataSource
import com.maciel.wavereaderkmm.processing.WaveDataProcessor
import com.maciel.wavereaderkmm.utils.nextBigWaveConfidence
import com.maciel.wavereaderkmm.utils.smoothOutput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI State for wave measurements
 */
data class WaveUiState(
    val measuredWaveList: List<MeasuredWaveData> = emptyList(),
    val height: Float? = null,
    val period: Float? = null,
    val direction: Float? = null,

    val isBelowThreshold: Boolean = false,

    val isRecording: Boolean = false,
    val recordingDuration: Float = 0f,

    val isSaving: Boolean = false,
    val lastSaveError: String? = null,
    val lastSaveSuccess: Boolean = false,

    val sensorsAvailable: Boolean = true,
    val sensorError: String? = null
)

/**
 * SensorViewModel
 *
 * Uses SensorDataSource (expect/actual) for platform-specific sensor access
 * All business logic is shared across Android and iOS
 */
class SensorViewModel(
    private val sensorDataSource: SensorDataSource,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaveUiState())
    val uiState: StateFlow<WaveUiState> = _uiState.asStateFlow()

    private val _bigWaveConfidence = MutableStateFlow(0f)
    val bigWaveConfidence: StateFlow<Float> = _bigWaveConfidence.asStateFlow()

    private val waveDataProcessor = WaveDataProcessor()

    private var currentLocationName: String = "Unknown location"
    private var currentLatLng: Pair<Double, Double>? = null

    private var startTime = 0L
    private var processingJob: Job? = null

    private var smoothedHeight: Float? = null
    private var smoothedPeriod: Float? = null

    /**
     * Check if sensors are available on this device
     */
    fun checkSensors(): Boolean {
        if (!sensorDataSource.areSensorsAvailable()) {
            _uiState.update {
                it.copy(
                    sensorsAvailable = false,
                    sensorError = "Required sensors not available"
                )
            }
            return false
        }
        return true
    }

    /**
     * Start collecting sensor data
     */
    fun startSensors() {
        if(!checkSensors()) return

        // Update UI state
        _uiState.update { it.copy(isRecording = true, sensorError = null) }
        startTime = currentTimeMs()

        // Start platform-specific sensor collection
        sensorDataSource.startListening { sensorData ->
            // Update sampling rate
            waveDataProcessor.updateSamplingRate(sensorData.samplingRate)

            // Add acceleration data to processor
            waveDataProcessor.addAccelerationData(
                vertical = sensorData.verticalAcceleration,
                horizontalX = sensorData.horizontalX,
                horizontalY = sensorData.horizontalY
            )
        }

        // Start periodic data processing (every 2 seconds)
        processingJob = viewModelScope.launch {
            while (isActive) {
                delay(2000L)

                val duration = (currentTimeMs() - startTime) / 1000f
                _uiState.update { it.copy(recordingDuration = duration) }

                processData()
            }
        }
    }

    /**
     * Stop collecting sensor data
     */
    fun stopSensors() {
        sensorDataSource.stopListening()
        processingJob?.cancel()
        _uiState.update { it.copy(isRecording = false) }
    }

    /**
     * Process accumulated sensor data
     */
    private fun processData() {
        val gyroDirection = sensorDataSource.getCurrentGyroDirection()

        val (avgHeight, avgPeriod, direction) = waveDataProcessor.processWaveData(gyroDirection) ?: Triple(0f, 0f, 0f)

        // Smooth the output
        smoothedHeight = smoothOutput(smoothedHeight, avgHeight)
        smoothedPeriod = smoothOutput(smoothedPeriod, avgPeriod)

        val elapsedTime = (currentTimeMs() - startTime) / 1000f

        // Update UI state
        updateMeasuredWaveData(
            smoothedHeight ?: avgHeight,
            smoothedPeriod ?: avgPeriod,
            direction,
            elapsedTime
        )

        // Update big wave confidence
        _bigWaveConfidence.value = nextBigWaveConfidence(_uiState.value.measuredWaveList)
    }

    /**
     * Update measured wave data list
     */
    private fun updateMeasuredWaveData(
        height: Float,
        period: Float,
        direction: Float,
        time: Float
    ) {
        AppLogger.i("Info", "RAW VALUES - Height: $height, Period: $period, Direction: $direction")
        _uiState.update { state ->
            val updated = state.measuredWaveList.toMutableList().apply {
                add(MeasuredWaveData(height, period, direction, time))
                if (size > 50) removeAt(0)
            }
            state.copy(
                measuredWaveList = updated,
                height = height,
                period = period,
                direction = direction
            )
        }
    }

    /**
     * Set current location for saving
     */
    fun setCurrentLocation(name: String, latLng: Pair<Double, Double>?) {
        currentLocationName = name
        currentLatLng = latLng
    }

    /**
     * Clear all measured wave data
     */
    fun clearMeasuredWaveData() {
        _uiState.update {
            it.copy(measuredWaveList = emptyList(), height = null, period = null, direction = null)
        }
        waveDataProcessor.clear()
        _bigWaveConfidence.value = 0f
        smoothedHeight = null
        smoothedPeriod = null
    }

    /**
     * Save wave session to Firestore
     */
    fun saveToFirestore(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, lastSaveError = null) }

            val result = firestoreRepository.saveSession(
                measuredData = uiState.value.measuredWaveList,
                locationName = currentLocationName,
                latLng = currentLatLng
            )

            result.onSuccess { docId ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSaveSuccess = true,
                        lastSaveError = null
                    )
                }
                onSuccess()
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSaveSuccess = false,
                        lastSaveError = exception.message ?: "Unknown error"
                    )
                }
                onFailure(exception as Exception)
            }
        }
    }

    fun clearSaveError() {
        _uiState.update { it.copy(lastSaveError = null, lastSaveSuccess = false) }
    }

    /**
     * Get current time in milliseconds
     */
    private fun currentTimeMs(): Long {
        return com.maciel.wavereaderkmm.TimeUtil.systemTimeMs()
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}