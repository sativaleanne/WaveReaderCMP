package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.GraphDisplayOptions
import com.maciel.wavereaderkmm.ui.components.AlertConfirm
import com.maciel.wavereaderkmm.ui.components.DropDownFilterGraphView
import com.maciel.wavereaderkmm.ui.components.SnackbarHelper
import com.maciel.wavereaderkmm.ui.components.WaveDataCard
import com.maciel.wavereaderkmm.ui.graph.SensorGraph
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.SensorViewModel
import com.maciel.wavereaderkmm.viewmodels.WaveUiState

/**
 * Record Screen
 */
@Composable
fun RecordDataScreen(
    sensorViewModel: SensorViewModel,
    locationViewModel: LocationViewModel,
    isGuest: Boolean
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbarHelper = remember { SnackbarHelper(snackbarHostState, scope) }

    val uiState by sensorViewModel.uiState.collectAsState()
    var isSensorActive by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (sensorViewModel.checkSensors()) {
                // Display Data
                ShowRecordData(uiState = uiState, viewModel = sensorViewModel)

                if (isSensorActive && uiState.measuredWaveList.isEmpty()) {
                    Text("Collecting Data...")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BUTTON ROW
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SensorButton(isSensorActive) { active ->
                        isSensorActive = active
                        if (active) {
                            sensorViewModel.startSensors()
                            snackbarHelper.showInfo("Recording started")
                        } else {
                            sensorViewModel.stopSensors()
                            snackbarHelper.showInfo("Recording paused")
                        }
                    }

                    if (uiState.measuredWaveList.isNotEmpty()) {
                        ClearButton(sensorViewModel, snackbarHelper)
                    }

                    if (uiState.measuredWaveList.isNotEmpty() && !isGuest && !isSensorActive) {
                        SaveButton(
                            sensorViewModel,
                            locationViewModel,
                            snackbarHelper
                        )
                    }
                }
            } else {
                ShowSensorErrorScreen()
                snackbarHelper.showError("Sensors unavailable")
            }
        }
    }
}

// Error if sensors unavailable
@Composable
fun ShowSensorErrorScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Unable to use this feature due to missing sensors!")
    }
}

// Start/Stop Sensors
@Composable
fun SensorButton(
    isSensorActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Button(
        modifier = Modifier.padding(8.dp),
        shape = RoundedCornerShape(9.dp),
        onClick = { onToggle(!isSensorActive) },
        elevation = ButtonDefaults.buttonElevation(1.dp)
    ) {
        Text(text = if (isSensorActive) "Pause" else "Record")
    }
}

// Save data to firebase with location
@Composable
fun SaveButton(
    sensorViewModel: SensorViewModel,
    locationViewModel: LocationViewModel,
    snackbarHelper: SnackbarHelper
) {
    var isSaving by remember { mutableStateOf(false) }

    Button(
        modifier = Modifier.padding(8.dp),
        shape = RoundedCornerShape(9.dp),
        onClick = {
            locationViewModel.fetchLocationAndSave(
                sensorViewModel = sensorViewModel,
                onSavingStarted = { isSaving = true },
                onSavingFinished = { isSaving = false },
                onSaveSuccess = {
                    snackbarHelper.showSuccess("Saved successfully!")
                }
            )
        },
        enabled = !isSaving,
        elevation = ButtonDefaults.buttonElevation(1.dp)
    ) {
        Text(if (isSaving) "Saving..." else "Save")
    }
}

// Clear data from graph and database
@Composable
fun ClearButton(
    viewModel: SensorViewModel,
    snackbarHelper: SnackbarHelper
) {
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        Button(
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(9.dp),
            onClick = { showDialog = true },
            elevation = ButtonDefaults.buttonElevation(1.dp)
        ) {
            Text("Clear")
        }

        if (showDialog) {
            AlertConfirm(
                onDismissRequest = { showDialog = false },
                onConfirmation = {
                    viewModel.clearMeasuredWaveData()
                    showDialog = false
                    snackbarHelper.showInfo("Data cleared")
                },
                dialogTitle = "Clear Data?",
                dialogText = "Are you sure you want to delete all recorded wave data?",
                icon = Icons.Default.Warning
            )
        }
    }
}

// Display data in graph and text
@Composable
fun ShowRecordData(
    uiState: WaveUiState,
    viewModel: SensorViewModel
) {
    var displayOptions by remember { mutableStateOf(GraphDisplayOptions()) }
    val confidence by viewModel.bigWaveConfidence.collectAsState()

    val height = uiState.height
    val period = uiState.period
    val direction = uiState.direction

    Column(modifier = Modifier.padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Display Wave Data
            WaveDataCard(
                "Average Conditions",
                listOf(height, period, direction),
                listOf("Height", "Period", "Direction"),
                listOf("ft", "s", "°")
            )

            // Filter Graph display
            DropDownFilterGraphView(displayOptions, onUpdate = { displayOptions = it })

            Column(modifier = Modifier.fillMaxWidth()) {
                SensorGraph(uiState.measuredWaveList, displayOptions)
            }
        }
    }
}