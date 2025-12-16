package com.maciel.wavereaderkmm.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciel.wavereaderkmm.data.WaveApiRepository
import com.maciel.wavereaderkmm.model.WaveApiQuery
import com.maciel.wavereaderkmm.model.WaveDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

/**
 * ServiceViewModel
 *
 * Handles wave data API calls using Ktor-based repository
 */
class ServiceViewModel(
    private val waveApiRepository: WaveApiRepository
) : ViewModel() {

    private val _serviceUiState = MutableStateFlow<UiState<WaveDataResponse>>(UiState.Empty)
    val serviceUiState: StateFlow<UiState<WaveDataResponse>> = _serviceUiState.asStateFlow()


    /**
     * Fetch wave data from API
     *
     * @param query Wave API query with location and parameters
     */
    fun fetchWaveData(query: WaveApiQuery) {
        viewModelScope.launch {
            _serviceUiState.value = UiState.Loading
            _serviceUiState.value = try {
                val data = waveApiRepository.getWaveApiData(query)
                UiState.Success(data)
            } catch (e: IOException) {
                UiState.Error("Network error: ${e.message}",
                exception = e)
            } catch (e: Exception) {
                UiState.Error(
                    message = "Unexpected error: ${e.message}",
                    exception = e
                )
            }
        }
    }

    /**
     * Reset to loading state
     */
    fun reset() {
        _serviceUiState.value = UiState.Empty
    }
}