package com.maciel.wavereaderkmm.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import kotlinx.coroutines.launch

/**
 * Reusable location search component that handles multiple input types:
 * - City names (e.g., "San Francisco")
 * - Coordinates (e.g., "37.7749, -122.4194")
 * - Zip codes (e.g., "94102")
 * - Current location via GPS
 *
 */
@Composable
fun LocationSearchField(
    locationViewModel: LocationViewModel,
    initialValue: String = "",
    label: String = "Search for a location",
    placeholder: String = "City, coordinates, or zip code",
    onLocationSelected: ((lat: Double, lon: Double, displayText: String) -> Unit)? = null,
    onTextChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showCurrentLocationButton: Boolean = true,
    showClearButton: Boolean = true,
    enabled: Boolean = true
) {
    var text by remember { mutableStateOf(initialValue) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                errorMessage = null
                onTextChanged?.invoke(it)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled,
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                when {
                    isProcessing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    text.isNotEmpty() && showClearButton -> {
                        IconButton(
                            onClick = {
                                text = ""
                                errorMessage = null
                                onTextChanged?.invoke("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (text.isNotBlank()) {
                        scope.launch {
                            handleLocationInput(
                                input = text.trim(),
                                locationViewModel = locationViewModel,
                                onStart = { isProcessing = true },
                                onSuccess = { lat, lon, displayText ->
                                    isProcessing = false
                                    errorMessage = null
                                    text = displayText  // Update field with formatted result
                                    onLocationSelected?.invoke(lat, lon, displayText)
                                },
                                onError = { error ->
                                    isProcessing = false
                                    errorMessage = error
                                }
                            )
                        }
                    }
                }
            )
        )

        if (showCurrentLocationButton) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        errorMessage = null

                        locationViewModel.fetchUserLocation()

                        // Wait a bit for the async operation to complete
                        // In a real app, you'd observe the ViewModel's StateFlow instead
                        kotlinx.coroutines.delay(1000)

                        // Get the result from ViewModel's state
                        val coordinates = locationViewModel.coordinatesState.value
                        val displayText = locationViewModel.displayLocationText.value
                        val hasError = locationViewModel.locationError.value

                        isProcessing = false

                        if (coordinates != null && !hasError) {
                            text = displayText
                            onLocationSelected?.invoke(
                                coordinates.latitude,
                                coordinates.longitude,
                                displayText
                            )
                        } else {
                            errorMessage = "Failed to get current location"
                        }
                    }
                },
                enabled = enabled && !isProcessing
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Use current location"
                )
            }
        }
    }
}

/**
 * Smart location input handler that detects input type and processes accordingly.
 *
 * Supported formats:
 * - Coordinates: "37.7749, -122.4194" or "37.7749,-122.4194"
 * - US Zip codes: "94102"
 * - Place names: "San Francisco", "Golden Gate Park", etc.
 */
private suspend fun handleLocationInput(
    input: String,
    locationViewModel: LocationViewModel,
    onStart: () -> Unit,
    onSuccess: (lat: Double, lon: Double, displayText: String) -> Unit,
    onError: (String) -> Unit
) {
    onStart()

    // Regex pattern for coordinates (format: lat,lon or lat, lon)
    val coordPattern = "^-?\\d+\\.?\\d*\\s*,\\s*-?\\d+\\.?\\d*$".toRegex()

    when {
        // Case 1: Input matches coordinate pattern
        coordPattern.matches(input) -> {
            val parts = input.split(",").map { it.trim().toDoubleOrNull() }
            if (parts.size == 2 && parts.all { it != null }) {
                val lat = parts[0]!!
                val lon = parts[1]!!

                // Validate coordinate ranges
                if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                    locationViewModel.setLocationFromMap(lat, lon)

                    // Wait briefly for reverse geocoding
                    kotlinx.coroutines.delay(500)

                    val displayText = locationViewModel.displayLocationText.value
                    onSuccess(lat, lon, displayText)
                } else {
                    onError("Coordinates out of valid range")
                }
            } else {
                onError("Invalid coordinate format")
            }
        }

        // Case 2: Treat as address/city/zip and geocode it
        else -> {
            geocodeLocation(input, locationViewModel, onSuccess, onError)
        }
    }
}

/**
 * Geocode a location query using LocationViewModel.
 *
 * This handles:
 * - City names: "San Francisco"
 * - Addresses: "1600 Amphitheatre Parkway, Mountain View"
 * - Zip codes: "94102"
 */
private suspend fun geocodeLocation(
    query: String,
    locationViewModel: LocationViewModel,
    onSuccess: (lat: Double, lon: Double, displayText: String) -> Unit,
    onError: (String) -> Unit
) {
    locationViewModel.selectLocation(query)

    // Wait briefly for geocoding to complete
    kotlinx.coroutines.delay(500)

    // Check ViewModel state for results
    val coordinates = locationViewModel.coordinatesState.value
    val displayText = locationViewModel.displayLocationText.value
    val hasError = locationViewModel.locationError.value

    if (coordinates != null && !hasError) {
        onSuccess(
            coordinates.latitude,
            coordinates.longitude,
            displayText
        )
    } else {
        onError("Location not found")
    }
}
