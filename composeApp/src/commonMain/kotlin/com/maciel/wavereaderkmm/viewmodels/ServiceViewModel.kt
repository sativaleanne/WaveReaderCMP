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
import kotlinx.io.IOException

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

    private val _isSearching = mutableStateOf(false)
    val isSearching: Boolean get() = _isSearching.value


    /**
     * Fetch wave data from API
     *
     * @param query Wave API query with location and parameters
     */
    fun fetchWaveData(query: WaveApiQuery) {
        viewModelScope.launch {
            _isSearching.value = true
            serviceUiState = UiState.Loading
            serviceUiState = try {
                val data = waveApiRepository.getWaveApiData(query)
                UiState.Success(data)
            } catch (e: IOException) {
                UiState.Error("Network error")
            } catch (e: Exception) {
                UiState.Error("Server error")
            } finally {
                _isSearching.value = false
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