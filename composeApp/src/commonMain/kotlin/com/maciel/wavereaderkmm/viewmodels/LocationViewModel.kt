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
 * UI State for Location operations
 *
 * Consolidates all location-related state into a single immutable data class.
 */
data class LocationUiState(
    val currentLocation: LocationData? = null,
    val displayText: String = "",
    val lastGeocodedAddress: GeocodedAddress? = null
)

/**
 * LocationViewModel
 */
class LocationViewModel(
    private val locationService: LocationService
) : ViewModel() {

    // Single source of truth for location state
    private val _uiState = MutableStateFlow<UiState<LocationUiState>>(
        UiState.Success(LocationUiState()) // Start with empty state
    )
    val uiState: StateFlow<UiState<LocationUiState>> = _uiState.asStateFlow()

    // Internal cache - not part of UI state
    private val geocodeCache = mutableMapOf<String, GeocodedAddress>()

    /**
     * Get current device location
     *
     * Using .onSuccess { } and .onFailure { }
     *
     */
    fun getCurrentLocation() {
        viewModelScope.launch {
            val currentState = getCurrentState()
            _uiState.value = UiState.Loading

            // Call the service - it returns Result<LocationData>
            locationService.getCurrentLocation()
                .onSuccess { location ->
                    // SUCCESS: location is LocationData

                    // Try to get address
                    var displayText = formatLatLong(location.latitude, location.longitude)

                    locationService.reverseGeocode(location.latitude, location.longitude)
                        .onSuccess { address ->
                            displayText = address
                        }
                        .onFailure {
                            // Keep the formatted coordinates if reverse geocode fails
                        }

                    // Update UI state with location
                    _uiState.value = UiState.Success(
                        LocationUiState(
                            currentLocation = location,
                            displayText = displayText,
                            lastGeocodedAddress = currentState.lastGeocodedAddress
                        )
                    )
                }
                .onFailure { error ->
                    // FAILURE: error is Throwable
                    _uiState.value = UiState.Error(
                        message = "Failed to get location: ${error.message}",
                        exception = error
                    )
                }
        }
    }

    /**
     * Geocode an address query (forward geocoding)
     *
     * Returns Result<T> for caller to handle with .onSuccess/.onFailure
     * This gives the caller flexibility in how to handle the result
     */
    suspend fun geocode(query: String): Result<GeocodedAddress> {
        // Check cache first
        geocodeCache[query]?.let {
            println("DEBUG: Cache hit for '$query'")
            return Result.success(it)
        }

        println("DEBUG: Cache miss for '$query', calling API")

        // Call API if not cached
        return locationService.geocodeAddress(query)
            .onSuccess { address ->
                // Cache successful result
                geocodeCache[query] = address
                println("DEBUG: Cached result for '$query'")
            }
    }

    /**
     * Set location from coordinates
     *
     * already have data (not fetching),
     * make async calls for related data
     */
    fun setLocation(latitude: Double, longitude: Double, displayText: String? = null) {
        viewModelScope.launch {
            val currentState = getCurrentState()
            val location = LocationData(latitude, longitude)

            // If no display text provided, try reverse geocode
            var text = displayText
            if (text == null) {
                locationService.reverseGeocode(latitude, longitude)
                    .onSuccess { address ->
                        text = address
                    }
                    .onFailure {
                        text = formatLatLong(latitude, longitude)
                    }
            }

            _uiState.value = UiState.Success(
                currentState.copy(
                    currentLocation = location,
                    displayText = text ?: formatLatLong(latitude, longitude)
                )
            )
        }
    }

    /**
     * Set location from geocoded address
     *
     * Direct state update
     */
    fun setLocationFromGeocoded(address: GeocodedAddress) {
        val currentState = getCurrentState()
        _uiState.value = UiState.Success(
            currentState.copy(
                currentLocation = LocationData(address.latitude, address.longitude),
                displayText = address.displayName,
                lastGeocodedAddress = address
            )
        )
    }

    /**
     * Reset location state
     */
    fun resetLocationState() {
        _uiState.value = UiState.Empty
    }

    /**
     * Clear error and return to last known good state
     */
    fun clearError() {
        val currentState = getCurrentState()
        _uiState.value = UiState.Success(currentState)
    }

    /**
     * Clear geocode cache
     */
    fun clearCache() {
        geocodeCache.clear()
        println("DEBUG: Geocode cache cleared")
    }

    // --- Helper Methods ---

    /**
     * Safely get current LocationUiState
     * Returns default empty state if not in Success state
     */
    private fun getCurrentState(): LocationUiState {
        return (_uiState.value as? UiState.Success)?.data ?: LocationUiState()
    }
}