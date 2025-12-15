package com.maciel.wavereaderkmm.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciel.wavereaderkmm.platform.GeocodedAddress
import com.maciel.wavereaderkmm.platform.LocationData
import com.maciel.wavereaderkmm.platform.LocationService
import com.maciel.wavereaderkmm.utils.formatLatLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LocationViewModel
 * Uses LocationService (expect/actual) for platform-specific location operations
 */
class LocationViewModel(
    private val locationService: LocationService
) : ViewModel() {

    private val _coordinatesState = MutableStateFlow<LocationData?>(null)
    val coordinatesState: StateFlow<LocationData?> = _coordinatesState.asStateFlow()

    private val geocodeCache = mutableMapOf<String, GeocodedAddress>()

    private val _locationError = MutableStateFlow(false)
    val locationError: StateFlow<Boolean> = _locationError.asStateFlow()

    private val _displayLocationText = MutableStateFlow("No location selected")
    val displayLocationText: StateFlow<String> = _displayLocationText.asStateFlow()

    fun resetLocationState() {
        _locationError.value = false
        _displayLocationText.value = "No location selected"
    }

    suspend fun geocode(query: String): Result<GeocodedAddress> {
        return geocodeCache[query]?.let { Result.success(it) }
            ?: locationService.geocodeAddress(query).also { result ->
                result.getOrNull()?.let { geocodeCache[query] = it }
            }
    }

    /**
     * Get current device location
     */
    fun fetchUserLocation() {
        viewModelScope.launch {
            locationService.getCurrentLocation()
                .onSuccess { location ->
                    _coordinatesState.value = location
                    _locationError.value = false

                    // Try to get human-readable address
                    locationService.reverseGeocode(location.latitude, location.longitude)
                        .onSuccess { address ->
                            _displayLocationText.value = address
                        }
                        .onFailure {
                            // Fallback to coordinates
                            _displayLocationText.value = formatLatLong(location.latitude, location.longitude)
                        }
                }
                .onFailure { error ->
                    _locationError.value = true
                    println("Location error: ${error.message}")
                }
        }
    }

    /**
     * Search for a location by name/address
     */
    fun selectLocation(placeName: String) {
        viewModelScope.launch {
            locationService.geocodeAddress(placeName)
                .onSuccess { geocoded ->
                    _coordinatesState.value = LocationData(
                        latitude = geocoded.latitude,
                        longitude = geocoded.longitude
                    )
                    _displayLocationText.value = geocoded.displayName
                    _locationError.value = false
                }
                .onFailure { error ->
                    _locationError.value = true
                    println("Geocoding error: ${error.message}")
                }
        }
    }

    /**
     * Set location from map click (coordinates only)
     */
    fun setLocationFromMap(lat: Double, lon: Double) {
        _coordinatesState.value = LocationData(lat, lon)

        viewModelScope.launch {
            // Try to get address for the coordinates
            locationService.reverseGeocode(lat, lon)
                .onSuccess { address ->
                    _displayLocationText.value = address
                }
                .onFailure {
                    // Fallback to coordinates
                    _displayLocationText.value = formatLatLong(lat, lon)
                }
        }
    }

    /**
     * Fetch location and save wave data
     * Used in RecordScreen when saving with location
     */
    fun fetchLocationAndSave(
        sensorViewModel: SensorViewModel,
        onSavingStarted: () -> Unit = {},
        onSavingFinished: () -> Unit = {},
        onSaveSuccess: () -> Unit = {}
    ) {
        onSavingStarted()

        viewModelScope.launch {
            locationService.getCurrentLocation()
                .onSuccess { location ->
                    val locationName = _displayLocationText.value
                    sensorViewModel.setCurrentLocation(
                        locationName,
                        Pair(location.latitude, location.longitude)
                    )

                    // Save to Firestore
                    sensorViewModel.saveToFirestore(
                        onSuccess = {
                            onSavingFinished()
                            onSaveSuccess()
                        },
                        onFailure = { error ->
                            onSavingFinished()
                            println("Save error: ${error.message}")
                        }
                    )
                }
                .onFailure { error ->
                    // Save without location
                    sensorViewModel.setCurrentLocation("Unknown Location", null)
                    sensorViewModel.saveToFirestore(
                        onSuccess = {
                            onSavingFinished()
                            onSaveSuccess()
                        },
                        onFailure = { error ->
                            onSavingFinished()
                            println("Save error: ${error.message}")
                        }
                    )
                }
        }
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _locationError.value = false
    }
}