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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.ApiVariable
import com.maciel.wavereaderkmm.model.FilterPreset
import com.maciel.wavereaderkmm.model.WaveApiQuery
import com.maciel.wavereaderkmm.model.WaveDataResponse
import com.maciel.wavereaderkmm.platform.MapView
import com.maciel.wavereaderkmm.ui.components.DropDownFilterSearchPresets
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
    val coordinates by locationViewModel.coordinatesState.collectAsState()
    val displayLocation by locationViewModel.displayLocationText.collectAsState()

    var isMapExpanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(FilterPreset.Wave) }
    var selectedVariables by remember { mutableStateOf(selectedPreset.variables) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search bar
        SearchForLocation(locationViewModel = locationViewModel)

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

        // Search + Filters Button
        DropDownFilterSearchPresets(
            selectedPreset = selectedPreset,
            onPresetSelected = {
                selectedPreset = it
                selectedVariables = it.variables
            }
        )

        Button(
            onClick = {
                coordinates?.let { location ->
                    val query = WaveApiQuery(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        variables = selectedVariables.ifEmpty {
                            setOf(
                                ApiVariable.WaveHeight,
                                ApiVariable.WaveDirection,
                                ApiVariable.WavePeriod
                            )
                        },
                        forecastDays = 1
                    )
                    serviceViewModel.fetchWaveData(query)
                }
            },
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            enabled = coordinates != null
        ) {
            Text("Search")
        }

        // Display Data or current state of searching data
        ShowSearchData(serviceViewModel.serviceUiState)
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
 * Search Field for getting location
 */
@Composable
fun SearchForLocation(locationViewModel: LocationViewModel) {
    var text by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Search for a place") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (text.isNotBlank()) {
                        locationViewModel.selectLocation(text)
                    }
                }
            )
        )

        // Find Current Location button
        IconButton(
            onClick = {
                locationViewModel.fetchUserLocation()
            }
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Use current location"
            )
        }
    }
}

/**
 * Display current state of searching data
 */
@Composable
fun ShowSearchData(serviceUiState: UiState<WaveDataResponse>) {
    when (serviceUiState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> SearchResultScreen(waveData = serviceUiState.data)
        is UiState.Error -> ErrorScreen(message = serviceUiState.message)
    }
}

/**
 * Show text and graph of data response from API
 */
@Composable
fun SearchResultScreen(
    waveData: WaveDataResponse,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        if (waveData.current?.waveHeight == null) {
            Text(
                text = "There is no wave data at this location!",
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WaveDataCard(
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
fun LoadingScreen(modifier: Modifier = Modifier) {
//    Image(
//        modifier = modifier.size(200.dp),
//        painter = painterResource(Res.drawable.loading_img),
//        contentDescription = stringResource(Res.string.loading_image_descr)
//    )
}

/**
 * Error screen
 */
@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Image(
//            painter = painterResource(Res.drawable.ic_connection_error),
//            contentDescription = stringResource(Res.string.error_image_descr)
//        )
        Text(
            text = message ?: stringResource(Res.string.loading_failed_text),
            modifier = Modifier.padding(16.dp)
        )
    }
}