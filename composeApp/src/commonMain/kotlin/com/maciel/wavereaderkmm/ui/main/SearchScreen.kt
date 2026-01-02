package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.ApiVariable
import com.maciel.wavereaderkmm.model.WaveApiQuery
import com.maciel.wavereaderkmm.model.WaveDataResponse
import com.maciel.wavereaderkmm.platform.LocationData
import com.maciel.wavereaderkmm.platform.MapView
import com.maciel.wavereaderkmm.ui.components.LoadingView
import com.maciel.wavereaderkmm.ui.components.LocationSearchField
import com.maciel.wavereaderkmm.ui.components.WaveDataCard
import com.maciel.wavereaderkmm.ui.graph.ServiceGraph
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.ServiceViewModel
import com.maciel.wavereaderkmm.viewmodels.UiState
import org.jetbrains.compose.resources.stringResource
import wavereaderkmm.composeapp.generated.resources.Res
import wavereaderkmm.composeapp.generated.resources.loading_failed_text

/**
 * Search Screen for retrieving wave data by location
 *
 */
@Composable
fun SearchDataScreen(
    locationViewModel: LocationViewModel,
    serviceViewModel: ServiceViewModel
) {
    val serviceUiState by serviceViewModel.serviceUiState.collectAsState()
    val locationUiState by locationViewModel.uiState.collectAsState()

    val locationData = (locationUiState as? UiState.Success)?.data
    val coordinates = locationData?.currentLocation
    val displayLocation = locationData?.displayText ?: "No location selected"

    // Local UI state
    var isMapExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LocationSearchField(
            locationViewModel = locationViewModel,
            onLocationSelected = { lat, lon, displayText ->
                println("Selected: $displayText at ($lat, $lon)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        when (locationUiState) {
            is UiState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Getting location...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is UiState.Error -> {
                Text(
                    text = "Location error: ${(locationUiState as UiState.Error).message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            is UiState.Success -> {
                // Show location info when available
                Text(
                    text = "Location: $displayLocation",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            is UiState.Empty -> {
                Text(
                    text = "No location selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ExpandableMapCard(
            locationViewModel = locationViewModel,
            isExpanded = isMapExpanded,
            onToggleExpanded = { isMapExpanded = !isMapExpanded },
            currentLocation = coordinates,
            displayText = displayLocation
        )

        SearchButton(
            uiState = serviceUiState,
            hasCoordinates = coordinates != null,
            onClick = {
                // Safely get coordinates from LocationUiState
                val location = (locationUiState as? UiState.Success)
                    ?.data
                    ?.currentLocation

                location?.let { loc ->
                    val query = WaveApiQuery(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        variables = setOf(
                            ApiVariable.WaveHeight,
                            ApiVariable.WavePeriod,
                            ApiVariable.WaveDirection
                        ),
                        forecastDays = 1
                    )
                    serviceViewModel.fetchWaveData(query)
                }
            }
        )

        // Wave data display (ServiceViewModel already uses UiState correctly)
        when (val state = serviceUiState) {
            is UiState.Loading -> LoadingView("Loading data...")

            is UiState.Success -> WaveDataDisplay(
                waveData = state.data,
                locationText = displayLocation,
                onRetry = { serviceViewModel.reset() }
            )

            is UiState.Error -> ErrorView(
                message = state.message ?: "Unknown error",
                onRetry = { serviceViewModel.reset() }
            )

            is UiState.Empty -> EmptyStateView()
        }
    }
}

/**
 * Search button with loading and enabled states
 */
@Composable
fun SearchButton(
    hasCoordinates: Boolean,
    uiState: UiState<WaveDataResponse>,
    onClick: () -> Unit
) {
    val isLoading = uiState is UiState.Loading
    val isEnabled = uiState is UiState.Empty && hasCoordinates

    Button(
        onClick = onClick,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(0.6f)
    ) {
        if (isLoading) {
            // Show loading indicator while searching
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching...")
            }
        } else {
            // Show search text
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isEnabled) {
                    Text("Search Wave Data")
                } else {
                    Text("Select location first")
                }
            }
        }
    }
}

/**
 * Expandable Map Card
 *
 */
@Composable
fun ExpandableMapCard(
    locationViewModel: LocationViewModel,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    currentLocation: LocationData?,
    displayText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Tap to Collapse Map" else "Tap to Expand Map",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 320.dp else 120.dp)
                    .padding(top = 8.dp)
            ) {
                MapView(
                    locationViewModel = locationViewModel,
                    coordinates = currentLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Show text and graph of data response from API
 */
@Composable
fun WaveDataDisplay(
    waveData: WaveDataResponse,
    locationText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        if (waveData.current?.waveHeight == null) {
            Text(
                text = "There is no wave data at $locationText!",
                fontWeight = FontWeight.Bold
            )
        }

        Button(onClick = onRetry) {
            Text("New Search")
        }

        Column {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WaveDataCard(
                    title = "Current Conditions at $locationText",
                    values = listOf(
                        waveData.current?.waveHeight,
                        waveData.current?.wavePeriod,
                        waveData.current?.waveDirection
                    ),
                    labels = listOf("Height", "Period", "Direction"),
                    units = listOf("m", "s", "°")
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                val hourly = waveData.hourly
                if (hourly?.time?.isNotEmpty() == true) {
                    ServiceGraph(hourly)
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No graph data available.")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



/**
 * Error screen
 */
@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message ?: stringResource(Res.string.loading_failed_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

/**
 * Empty state when no location selected
 */
@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Search for a location to get wave data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}