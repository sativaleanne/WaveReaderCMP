package com.maciel.wavereaderkmm.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciel.wavereaderkmm.data.WaveApiRepository
import com.maciel.wavereaderkmm.model.WaveApiQuery
import com.maciel.wavereaderkmm.model.WaveDataResponse
import kotlinx.coroutines.launch

/**
 * ServiceViewModel
 *
 * Handles wave data API calls using Ktor-based repository
 */
class ServiceViewModel(
    private val waveApiRepository: WaveApiRepository
) : ViewModel() {

    var serviceUiState: UiState<WaveDataResponse> by mutableStateOf(UiState.Loading)
        private set

    /**
     * Fetch wave data from API
     *
     * @param query Wave API query with location and parameters
     */
    fun fetchWaveData(query: WaveApiQuery) {
        viewModelScope.launch {
            serviceUiState = UiState.Loading
            serviceUiState = try {
                val data = waveApiRepository.getWaveApiData(query)
                UiState.Success(data)
            } catch (e: Exception) {
                // Generic exception handling
                when {
                    e.message?.contains("Unable to resolve host") == true ||
                            e.message?.contains("timeout") == true ||
                            e.message?.contains("connection") == true -> {
                        UiState.Error("Network error - please check your connection")
                    }
                    e.message?.contains("500") == true ||
                            e.message?.contains("503") == true -> {
                        UiState.Error("Server error - please try again later")
                    }
                    else -> {
                        UiState.Error("Error: ${e.message ?: "Unknown error"}")
                    }
                }
            }
        }
    }

    /**
     * Reset to loading state
     */
    fun reset() {
        serviceUiState = UiState.Loading
    }
}