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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.model.SortOrder
import com.maciel.wavereaderkmm.utils.formatDate

/**
 * History Filter Panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterPanel(
    initialFilter: HistoryFilterState = HistoryFilterState(),
    onApply: (HistoryFilterState) -> Unit
) {
    // State for filter options
    var locationText by remember { mutableStateOf(initialFilter.locationQuery) }
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

        // Clear and Apply buttons
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clear Button
            Button(
                onClick = { onApply(HistoryFilterState()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Apply Filter Button
            Button(
                onClick = {
                    onApply(
                        HistoryFilterState(
                            locationQuery = locationText.trim(),
                            startDateMillis = startDateMillis,
                            endDateMillis = endDateMillis,
                            sortOrder = sortOrder
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Filter")
            }
        }
    }
}