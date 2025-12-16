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
import com.maciel.wavereaderkmm.platform.MapView
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
 */
@Composable
fun SearchDataScreen(
    locationViewModel: LocationViewModel,
    serviceViewModel: ServiceViewModel
) {

    val uiState by serviceViewModel.serviceUiState.collectAsState()
    val coordinates by locationViewModel.coordinatesState.collectAsState()
    val displayLocation by locationViewModel.displayLocationText.collectAsState()
    var isMapExpanded by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Search Component
        LocationSearchField(
            locationViewModel = locationViewModel,
            label = "Search for a location",
            placeholder = "City, coordinates, or zip code",
            onLocationSelected = { lat, lon, displayText ->
                // Location automatically updates in LocationViewModel
                println("Selected: $displayText at ($lat, $lon)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Display Location info
        Text(
            text = "Location: $displayLocation",
            style = MaterialTheme.typography.labelMedium
        )

        // Expandable Map Display Card
        ExpandableMapCard(
            isExpanded = isMapExpanded,
            onToggleExpanded = { isMapExpanded = !isMapExpanded },
            locationViewModel = locationViewModel
        )

        SearchButton(
            uiState = uiState,
            hasCoordinates = coordinates != null,
            onClick = {
                coordinates?.let { (lat, lon) ->
                    val query = WaveApiQuery(
                        latitude = lat,
                        longitude = lon,
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

        when (val state = uiState) {
            is UiState.Loading -> LoadingView()

            is UiState.Success -> WaveDataDisplay(
                waveData = state.data,
                locationText = displayLocation,
                onRetry = { serviceViewModel.reset() }
            )

            is UiState.Error -> ErrorView(
                message = state.message ?: "Unknown error",
                onRetry = { serviceViewModel.reset() },
            )

            is UiState.Empty -> EmptyStateView()
        }
    }
}

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
        modifier = Modifier.fillMaxWidth(0.6f) // Make it a bit wider for better visibility
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
            // Show search text with icon
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isEnabled) {
                    Text("Search Wave Data")
                } else {
                    Text("No location selected")
                }
            }
        }
    }
}

/**
 * Expandable Map Card
 */
@Composable
fun ExpandableMapCard(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    locationViewModel: LocationViewModel
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
                    contentDescription = "Expand Button"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 320.dp else 120.dp)
                    .padding(top = 8.dp)
            ) {
                // Platform-specific map (expect/actual)
                MapView(
                    locationViewModel = locationViewModel,
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

        Column(modifier = Modifier.padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WaveDataCard(
                    "Current Conditions at $locationText",
                    values = listOf(
                        waveData.current?.waveHeight,
                        waveData.current?.wavePeriod,
                        waveData.current?.waveDirection
                    ),
                    labels = listOf("Height", "Period", "Direction"),
                    units = listOf("ft", "s", "°")
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                val hourly = waveData.hourly
                if (hourly?.time?.isNotEmpty() == true) {
                    ServiceGraph(hourly)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text("No graph data available.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Loading screen
 */
@Composable
fun LoadingView() {
    CircularProgressIndicator(
        modifier = Modifier.padding(16.dp)
    )
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: stringResource(Res.string.loading_failed_text),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun EmptyStateView() {
    Text(
        text = "Please select a location to search",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 4.dp)
    )
}