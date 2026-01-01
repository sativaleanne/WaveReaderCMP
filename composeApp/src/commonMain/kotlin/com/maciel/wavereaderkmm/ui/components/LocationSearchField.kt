package com.maciel.wavereaderkmm.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.UiState
import kotlinx.coroutines.launch

/**
 * Reusable location search component that handles multiple input types:
 * - City names (e.g., "San Francisco")
 * - Coordinates (e.g., "37.7749, -122.4194")
 * - Place names
 * - Current location via GPS
 *
 * Works with the refactored LocationViewModel using UIState pattern
 */
@Composable
fun LocationSearchField(
    locationViewModel: LocationViewModel,
    onLocationSelected: ((Double, Double, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var searchText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Collect UIState
    val uiState by locationViewModel.uiState.collectAsState()

    // Derive display state from UIState
    val isProcessing = uiState is UiState.Loading
    val uiErrorMessage = (uiState as? UiState.Error)?.message
    val currentLocationData = when (val state = uiState) {
        is UiState.Success -> state.data
        else -> null
    }

    // Update search text when location is set
    LaunchedEffect(currentLocationData?.displayText) {
        currentLocationData?.displayText?.let { displayText ->
            searchText = displayText
        }
    }

    // React to successful location selection
    LaunchedEffect(currentLocationData?.currentLocation) {
        currentLocationData?.currentLocation?.let { location ->
            errorMessage = null // Clear any previous errors
            onLocationSelected?.invoke(
                location.latitude,
                location.longitude,
                currentLocationData.displayText
            )
        }
    }

    // React to errors from ViewModel
    LaunchedEffect(uiErrorMessage) {
        uiErrorMessage?.let { errorMessage = it }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                errorMessage = null // Clear error when user types
            },
            placeholder = { Text("City, address, or coordinates") },
            modifier = Modifier.weight(1f),
            enabled = enabled && !isProcessing,
            isError = errorMessage != null,
            supportingText = {
                errorMessage?.let { Text(it) }
            },
            leadingIcon = {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        if (searchText.isNotBlank()) {
                            scope.launch {
                                handleLocationInput(
                                    input = searchText.trim(),
                                    locationViewModel = locationViewModel,
                                    onError = { error -> errorMessage = error }
                                )
                            }
                        }
                    },
                    enabled = enabled && !isProcessing && searchText.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, "Search")
                }
            },
            trailingIcon = {
                when {
                    isProcessing -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    errorMessage != null -> {
                        IconButton(onClick = {
                            errorMessage = null
                            locationViewModel.clearError()
                        }) {
                            Icon(Icons.Default.Error, "Clear error")
                        }
                    }
                    searchText.isNotEmpty() -> {
                        IconButton(onClick = {
                            searchText = ""
                            locationViewModel.resetLocationState()
                        }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    if (searchText.isNotBlank()) {
                        scope.launch {
                            handleLocationInput(
                                input = searchText.trim(),
                                locationViewModel = locationViewModel,
                                onError = { error -> errorMessage = error }
                            )
                        }
                    }
                }
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Current location button
        IconButton(
            onClick = { locationViewModel.getCurrentLocation() },
            enabled = enabled && !isProcessing
        ) {
            Icon(Icons.Default.MyLocation, "Current location")
        }
    }
}

/**
 * Smart location input handler that detects input type and processes accordingly.
 *
 * Supported formats:
 * - Coordinates: "37.7749, -122.4194" or "37.7749,-122.4194"
 * - Place names: "San Francisco", "Golden Gate Park", etc.
 *
 * Now works with LocationViewModel's UIState pattern
 */
private suspend fun handleLocationInput(
    input: String,
    locationViewModel: LocationViewModel,
    onError: (String) -> Unit
) {
    // Regex pattern for coordinates (format: lat,lon or lat, lon)
    val coordPattern = "^-?\\d+\\.?\\d*\\s*,\\s*-?\\d+\\.?\\d*$".toRegex()

    when {
        // Case 1: Input matches coordinate pattern
        coordPattern.matches(input) -> {
            handleCoordinateInput(input, locationViewModel, onError)
        }

        // Case 2: Treat as address/city and geocode it
        else -> {
            handleGeocodeInput(input, locationViewModel, onError)
        }
    }
}

/**
 * Handle direct coordinate input
 */
private fun handleCoordinateInput(
    input: String,
    locationViewModel: LocationViewModel,
    onError: (String) -> Unit
) {
    val parts = input.split(",").map { it.trim().toDoubleOrNull() }

    if (parts.size == 2 && parts.all { it != null }) {
        val lat = parts[0]!!
        val lon = parts[1]!!

        // Validate coordinate ranges
        if (lat in -90.0..90.0 && lon in -180.0..180.0) {
            // Set location directly with coordinates
            locationViewModel.setLocation(lat, lon)
        } else {
            onError("Coordinates out of valid range (lat: -90 to 90, lon: -180 to 180)")
        }
    } else {
        onError("Invalid coordinate format. Use: latitude, longitude")
    }
}

/**
 * Handle geocoding of place names, addresses, etc.
 *
 * Now properly uses the new LocationViewModel API
 */
private suspend fun handleGeocodeInput(
    query: String,
    locationViewModel: LocationViewModel,
    onError: (String) -> Unit
) {
    // Call geocode method which returns Result<GeocodedAddress>
    locationViewModel.geocode(query)
        .onSuccess { geocodedAddress ->
            // Set location using the geocoded address
            locationViewModel.setLocationFromGeocoded(geocodedAddress)
        }
        .onFailure { error ->
            onError("Location not found: ${error.message ?: "Unknown error"}")
        }
}
