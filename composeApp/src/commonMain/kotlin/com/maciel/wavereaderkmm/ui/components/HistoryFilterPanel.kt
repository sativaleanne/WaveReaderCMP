package com.maciel.wavereaderkmm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.model.SortOrder
import com.maciel.wavereaderkmm.utils.formatDate
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel

/**
 * History Filter Panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterPanel(
    initialFilter: HistoryFilterState = HistoryFilterState(),
    locationViewModel: LocationViewModel,
    onApply: (HistoryFilterState) -> Unit
) {
    // State for filter options
    var locationText by remember { mutableStateOf(initialFilter.locationQuery) }
    var searchLatLng by remember { mutableStateOf(initialFilter.searchLatLng) }
    var useLocationFilter by remember { mutableStateOf(initialFilter.searchLatLng != null) }
    var radiusMiles by remember { mutableStateOf(initialFilter.radiusMiles.toFloat()) }

    var startDateMillis by remember { mutableStateOf(initialFilter.startDateMillis) }
    var endDateMillis by remember { mutableStateOf(initialFilter.endDateMillis) }

    // Format dates for display
    var startDateText by remember {
        mutableStateOf(
            initialFilter.startDateMillis?.let { formatDate(it) } ?: ""
        )
    }
    var endDateText by remember {
        mutableStateOf(
            initialFilter.endDateMillis?.let { formatDate(it) } ?: ""
        )
    }

    var sortOrder by remember { mutableStateOf(initialFilter.sortOrder) }
    var expandedSort by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter by Location",
                style = MaterialTheme.typography.titleSmall
            )
            Switch(
                checked = useLocationFilter,
                onCheckedChange = {
                    useLocationFilter = it
                    if (!it) {
                        locationText = ""
                        searchLatLng = null
                        locationViewModel.resetLocationState()
                    }
                }
            )
        }

        // Location Search (only shown when enabled)
        if (useLocationFilter) {
            LocationSearchField(
                locationViewModel = locationViewModel,
                initialValue = locationText,
                label = "Search location",
                placeholder = "City, coordinates, or zip code",
                onLocationSelected = { lat, lon, displayText ->
                    println("DEBUG: Location selected")
                    println("  - Coordinates: ($lat, $lon)")
                    println("  - Display: $displayText")

                    // Store coordinates for filtering
                    searchLatLng = Pair(lat, lon)

                    // Store display text for user reference
                    locationText = displayText
                },
                onTextChanged = { newText ->
                    locationText = newText
                    if (newText.isEmpty()) {
                        searchLatLng = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            // Show radius slider whenever we have coordinates
            if (searchLatLng != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Search radius: ${radiusMiles.toInt()} miles",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = radiusMiles,
                        onValueChange = { radiusMiles = it },
                        valueRange = 5f..100f,
                        steps = 18
                    )
                    Text(
                        text = "Searching within ${radiusMiles.toInt()} miles of: $locationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Date Range Picker Button
        TextButton(onClick = { showDateRangePicker = true }) {
            Text(
                text = if (startDateText.isNotBlank() && endDateText.isNotBlank()) {
                    "From $startDateText to $endDateText"
                } else {
                    "Select Date Range"
                }
            )
        }

        // Date Range Picker Dialog
        if (showDateRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            startDateMillis = dateRangePickerState.selectedStartDateMillis
                            endDateMillis = dateRangePickerState.selectedEndDateMillis
                            startDateText = startDateMillis?.let { formatDate(it) } ?: ""
                            endDateText = endDateMillis?.let { formatDate(it) } ?: ""
                            showDateRangePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    showModeToggle = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .padding(16.dp)
                )
            }
        }

        // Sort Order Dropdown
        OutlinedButton(
            onClick = { expandedSort = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sort: ${if (sortOrder == SortOrder.DATE_DESCENDING) "Newest First" else "Oldest First"}"
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expandedSort,
            onDismissRequest = { expandedSort = false }
        ) {
            DropdownMenuItem(
                text = { Text("Newest First") },
                onClick = {
                    sortOrder = SortOrder.DATE_DESCENDING
                    expandedSort = false
                }
            )
            DropdownMenuItem(
                text = { Text("Oldest First") },
                onClick = {
                    sortOrder = SortOrder.DATE_ASCENDING
                    expandedSort = false
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clear and Apply buttons
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clear Button
            OutlinedButton(
                onClick = {
                    locationText = ""
                    searchLatLng = null
                    useLocationFilter = false
                    radiusMiles = 25f
                    startDateMillis = null
                    endDateMillis = null
                    startDateText = ""
                    endDateText = ""
                    sortOrder = SortOrder.DATE_DESCENDING
                    locationViewModel.resetLocationState()
                    onApply(HistoryFilterState())
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear All")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Apply Filter Button
            Button(
                onClick = {
                    val newFilter = HistoryFilterState(
                        locationQuery = locationText.trim(), // Just for display
                        searchLatLng = if (useLocationFilter) searchLatLng else null, // Actual filter
                        radiusMiles = radiusMiles.toDouble(),
                        startDateMillis = startDateMillis,
                        endDateMillis = endDateMillis,
                        sortOrder = sortOrder
                    )

                    println("DEBUG: Creating filter:")
                    println("  - Display text: ${newFilter.locationQuery}")
                    println("  - Coordinates: ${newFilter.searchLatLng}")
                    println("  - Radius: ${newFilter.radiusMiles} miles")

                    onApply(newFilter)
                },
                modifier = Modifier.weight(1f),
                enabled = !useLocationFilter || searchLatLng != null
            ) {
                Text("Apply Filter")
            }
        }
    }
}