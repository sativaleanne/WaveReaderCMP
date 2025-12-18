package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.GraphDisplayOptions
import com.maciel.wavereaderkmm.ui.components.AlertConfirm
import com.maciel.wavereaderkmm.ui.components.DropDownFilterGraphView
import com.maciel.wavereaderkmm.ui.components.WaveDataCard
import com.maciel.wavereaderkmm.ui.graph.SensorGraph
import com.maciel.wavereaderkmm.utils.formatDuration
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.SensorViewModel
import com.maciel.wavereaderkmm.viewmodels.UiState
import com.maciel.wavereaderkmm.viewmodels.WaveUiState
import kotlinx.coroutines.launch

/**
 * Record Screen
 *
 * Shows real-time wave measurements from device sensors
 */
@Composable
fun RecordDataScreen(
    sensorViewModel: SensorViewModel,
    locationViewModel: LocationViewModel,
    isGuest: Boolean
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by sensorViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.lastSaveSuccess) {
        if (uiState.lastSaveSuccess) {
            snackbarHostState.showSnackbar(
                message = "Wave session saved successfully!",
                duration = SnackbarDuration.Short
            )
            sensorViewModel.clearSaveError()
        }
    }

    LaunchedEffect(uiState.lastSaveError) {
        uiState.lastSaveError?.let { error ->
            snackbarHostState.showSnackbar(
                message = "Save failed: $error",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.sensorsAvailable) {
                SensorUnavailableBanner(uiState.sensorError)
                return@Column
            }

            if (!uiState.isRecording && uiState.measuredWaveList.isEmpty() && uiState.sensorsAvailable) {
                Text("Press Record to start collecting wave data.")
            }

            if (uiState.isRecording) {
                RecordingIndicator(duration = uiState.recordingDuration)
            }

            if (uiState.isRecording && uiState.measuredWaveList.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Collecting data...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.measuredWaveList.isNotEmpty()) {
                WaveDataDisplay(
                    uiState = uiState,
                    viewModel = sensorViewModel
                )
            }

            if (uiState.isSaving) {
                Spacer(modifier = Modifier.height(16.dp))
                SavingIndicator()
            }

            Spacer(modifier = Modifier.height(24.dp))

            ActionButtons(
                uiState = uiState,
                viewModel = sensorViewModel,
                locationViewModel = locationViewModel,
                isGuest = isGuest
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Banner shown when sensors are unavailable
 */
@Composable
private fun SensorUnavailableBanner(errorMessage: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sensors Unavailable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage ?: "Required sensors are not available on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Recording indicator with live duration
 */
@Composable
private fun RecordingIndicator(duration: Float) {
    Row {
        Text(
            text = "Recording: ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * Saving indicator
 */
@Composable
private fun SavingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Saving...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Wave data display with graph and metrics
 */
@Composable
private fun WaveDataDisplay(
    uiState: WaveUiState,
    viewModel: SensorViewModel
) {
    var displayOptions by remember { mutableStateOf(GraphDisplayOptions()) }
    val confidence by viewModel.bigWaveConfidence.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WaveDataCard(
            title = "Current Conditions",
            values = listOf(uiState.height, uiState.period, uiState.direction),
            labels = listOf("Height", "Period", "Direction"),
            units = listOf("ft", "s", "°")
        )

        Spacer(modifier = Modifier.height(16.dp))

        DropDownFilterGraphView(
            displayOptions = displayOptions,
            onUpdate = { displayOptions = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SensorGraph(
            waveData = uiState.measuredWaveList,
            display = displayOptions
        )
    }
}

/**
 * Action buttons for record/save/clear
 *
 * ✅ REFACTORED: Now properly orchestrates location fetch and save
 */
@Composable
private fun ActionButtons(
    uiState: WaveUiState,
    viewModel: SensorViewModel,
    locationViewModel: LocationViewModel,
    isGuest: Boolean
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var isFetchingLocationForSave by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ✅ NEW: Collect location state
    val locationUiState by locationViewModel.uiState.collectAsState()

    // ✅ NEW: Handle save flow when location is ready
    LaunchedEffect(locationUiState, isFetchingLocationForSave) {
        if (!isFetchingLocationForSave) return@LaunchedEffect

        when (val state = locationUiState) {
            is UiState.Success -> {
                val locationData = state.data
                val location = locationData.currentLocation

                // Set location and save
                viewModel.setCurrentLocation(
                    name = locationData.displayText,
                    latLng = location?.let { it.latitude to it.longitude }
                )

                viewModel.saveToFirestore(
                    onSuccess = {
                        isFetchingLocationForSave = false
                    },
                    onFailure = {
                        isFetchingLocationForSave = false
                    }
                )
            }

            is UiState.Error -> {
                // Location failed - save anyway with "Unknown location"
                viewModel.setCurrentLocation("Unknown location", null)
                viewModel.saveToFirestore(
                    onSuccess = {
                        isFetchingLocationForSave = false
                    },
                    onFailure = {
                        isFetchingLocationForSave = false
                    }
                )
            }

            else -> { /* Loading or Empty - wait */ }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Record/Stop button
        Button(
            onClick = {
                if (uiState.isRecording) {
                    viewModel.stopSensors()
                } else {
                    viewModel.startSensors()
                }
            },
            enabled = uiState.sensorsAvailable && !uiState.isSaving,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = if (uiState.isRecording) "Stop" else "Record")
        }

        Spacer(modifier = Modifier.size(8.dp))

        // Save button
        if (!isGuest) {
            Button(
                onClick = {
                    // ✅ NEW: Trigger location fetch and save
                    scope.launch {
                        isFetchingLocationForSave = true
                        locationViewModel.getCurrentLocation()
                    }
                },
                enabled = !uiState.isSaving &&
                        !uiState.isRecording &&
                        uiState.measuredWaveList.isNotEmpty() &&
                        !isFetchingLocationForSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isFetchingLocationForSave && locationUiState is UiState.Loading) {
                    // Show loading during location fetch
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saving...")
                    }
                } else {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.size(8.dp))
        }

        // Clear button
        if (uiState.measuredWaveList.isNotEmpty()) {
            Button(
                onClick = { showClearDialog = true },
                enabled = !uiState.isRecording &&
                        uiState.measuredWaveList.isNotEmpty() &&
                        !uiState.isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear")
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertConfirm(
            onDismissRequest = { showClearDialog = false },
            onConfirmation = {
                viewModel.clearMeasuredWaveData()
                showClearDialog = false
            },
            dialogTitle = "Clear Data?",
            dialogText = "Are you sure you want to delete all recorded wave data? This cannot be undone.",
            icon = Icons.Default.Warning
        )
    }
}