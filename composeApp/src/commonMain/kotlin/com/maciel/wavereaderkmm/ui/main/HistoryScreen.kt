package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.ui.components.HistoryFilterPanel
import com.maciel.wavereaderkmm.ui.graph.HistoryGraph
import com.maciel.wavereaderkmm.utils.toDecimalString
import com.maciel.wavereaderkmm.viewmodels.HistoryViewModel
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.UiState

/**
 * History Screen - View saved wave sessions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun HistoryScreen(
    viewModel: HistoryViewModel,
    locationViewModel: LocationViewModel,
    onBack: () -> Unit
)

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun HistoryScreen(
//    viewModel: HistoryViewModel,
//    locationViewModel: LocationViewModel,
//    onBack: () -> Unit
//) {
//    // Collect UIState
//    val uiState by viewModel.uiState.collectAsState()
//
//    var showExportDialog by remember { mutableStateOf(false) }
//
//    val snackbarHostState = remember { SnackbarHostState() }
//    val scope = rememberCoroutineScope()
//    val snackbar = remember { SnackbarHelper(snackbarHostState, scope) }
//
//    Scaffold(
//        topBar = {
//            if (uiState is UiState.Success && !(uiState as UiState.Success).data.isSelectionMode) {
//                TopAppBar(
//                    title = { Text("History") },
//                    navigationIcon = {
//                        IconButton(onClick = onBack) {
//                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
//                        }
//                    },
//                    actions = {
//                        // Only show filter button when we have data
//                        DropDownFilterButton(viewModel, locationViewModel)
//                        IconButton(onClick = { showExportDialog = true }) {
//                            Icon(
//                                imageVector = Icons.Default.Download,
//                                contentDescription = "Export all visible records"
//                            )
//                        }
//                    }
//                )
//            }
//            // Selection mode top app bar (shown when IN selection mode)
//            if (uiState is UiState.Success && (uiState as UiState.Success).data.isSelectionMode) {
//                val selectedCount = (uiState as UiState.Success).data.selectedItems.size
//                TopAppBar(
//                    title = { Text("$selectedCount selected") },
//                    navigationIcon = {
//                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
//                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection mode")
//                        }
//                    },
//                    actions = {
//                        // EXPORT SELECTED BUTTON - in selection bar
//                        IconButton(
//                            onClick = { showExportDialog = true },
//                            enabled = selectedCount > 0
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Download,
//                                contentDescription = "Export selected records"
//                            )
//                        }
//
//                        // Delete selected button
//                        IconButton(
//                            onClick = { viewModel.deleteSelectedRecords() },
//                            enabled = selectedCount > 0
//                        ) {
//                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
//                        }
//
//                        // Select all button
//                        IconButton(onClick = { if (selectedCount > 0) {viewModel.deselectAll()} else {viewModel.selectAll()} }) {
//                            if (selectedCount > 0) {
//                                Icon(Icons.Default.Deselect, contentDescription = "Deselect all")
//                            } else {
//                                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
//                            }
//                        }
//                    }
//                )
//            }
//        }
//    ) { paddingValues ->
//        Box(modifier = Modifier.padding(paddingValues)) {
//            // Exhaustive when expression
//            when (val state = uiState) {
//                is UiState.Loading -> {
//                    LoadingView()
//                }
//
//                is UiState.Success -> {
//                    val historyState = state.data
//
//                    // Show empty state if no records after filtering
//                    if (historyState.historyRecords.isEmpty()) {
//                        EmptyHistoryView(
//                            filterState = historyState.filterState,
//                            onClearFilters = {
//                                viewModel.setDefaultRecentFilter()
//                                viewModel.refresh()
//                            }
//                        )
//                    } else {
//                        Column {
//                            // Show active filter info
//                            if (historyState.filterState.searchLatLng != null) {
//                                ActiveFilterInfo(historyState.filterState)
//                            }
//                            // History list
//                            HistoryList(
//                                records = historyState.historyRecords,
//                                expandedItems = historyState.expandedItems,
//                                isSelectionMode = historyState.isSelectionMode,
//                                selectedItems = historyState.selectedItems,
//                                onItemClick = { id -> viewModel.toggleItemExpansion(id) },
//                                onItemSelect = { id -> viewModel.toggleItemSelection(id) },
//                                onItemLongClick = { viewModel.toggleSelectionMode() },
//                                onDeleteRecord = { id -> viewModel.deleteRecord(id) }
//                            )
//
//                            // This automatically handles both "export all" and "export selected"
//                            SmartExportDialog(
//                                showDialog = showExportDialog,
//                                isSelectionMode = historyState.isSelectionMode,
//                                selectedCount = historyState.selectedItems.size,
//                                allVisibleCount = historyState.historyRecords.size,
//                                onDismiss = { showExportDialog = false },
//
//                                // Export SELECTED as CSV
//                                onExportSelectedCsv = {
//                                    viewModel.exportSelectedToCsv(
//                                        onSuccess = { message ->
//                                            snackbar.showSuccess(message)
//                                        },
//                                        onFailure = { error ->
//                                            snackbar.showError(error)
//                                        }
//                                    )
//                                },
//
//                                // Export SELECTED as JSON
//                                onExportSelectedJson = {
//                                    viewModel.exportSelectedToJson(
//                                        onSuccess = { message ->
//                                            snackbar.showSuccess(message)
//                                        },
//                                        onFailure = { error ->
//                                            snackbar.showError(error)
//                                        }
//                                    )
//                                },
//
//                                // Export ALL as CSV
//                                onExportAllCsv = {
//                                    viewModel.exportAllToCsv(
//                                        onSuccess = { message ->
//                                            snackbar.showSuccess(message)
//                                        },
//                                        onFailure = { error ->
//                                            snackbar.showError(error)
//                                        }
//                                    )
//                                },
//
//                                // Export ALL as JSON
//                                onExportAllJson = {
//                                    viewModel.exportAllToJson(
//                                        onSuccess = { message ->
//                                            snackbar.showSuccess(message)
//                                        },
//                                        onFailure = { error ->
//                                            snackbar.showError(error)
//                                        }
//                                    )
//                                }
//                            )
//                        }
//                    }
//                }
//
//                is UiState.Error -> {
//                    ErrorView(
//                        message = state.message ?: "Unknown error",
//                        onRetry = { viewModel.clearError() },
//                        onBack = onBack
//                    )
//                }
//
//                is UiState.Empty -> {
//                    EmptyHistoryView(
//                        filterState = HistoryFilterState(),
//                        onClearFilters = { viewModel.setDefaultRecentFilter()
//                        viewModel.refresh()
//                        }
//                    )
//                }
//            }
//        }
//    }
//}

/**
 * History list component
 */
@Composable
fun HistoryList(
    records: List<HistoryRecord>,
    expandedItems: Set<String>,
    isSelectionMode: Boolean,
    selectedItems: Set<String>,
    onItemClick: (String) -> Unit,
    onItemSelect: (String) -> Unit,
    onItemLongClick: () -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = records,
            key = { it.id }
        ) { record ->
            HistoryCard(
                record = record,
                isExpanded = record.id in expandedItems,
                isSelected = record.id in selectedItems,
                isSelectionMode = isSelectionMode,
                onToggle = { onItemClick(record.id) },
                onClick = {
                    if (isSelectionMode) {
                        onItemSelect(record.id)
                    } else {
                        onItemClick(record.id)
                    }
                },
                onLongClick = onItemLongClick
            )
        }
    }
}

/**
 * Empty history view with filter awareness
 */
@Composable
fun EmptyHistoryView(
    filterState: HistoryFilterState,
    onClearFilters: () -> Unit
) {
    // Check if any filters are active
    val hasActiveFilter = filterState.searchLatLng != null ||
            filterState.startDateMillis != null ||
            filterState.endDateMillis != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasActiveFilter) "No matching records found" else "No history yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasActiveFilter) {
                buildString {
                    append("No records found")
                    if (filterState.searchLatLng != null) {
                        append(" within ${filterState.radiusMiles.toInt()} miles of ${filterState.locationQuery}")
                    }
                    if (filterState.startDateMillis != null || filterState.endDateMillis != null) {
                        append(" in the selected date range")
                    }
                    append(".\n\nTry adjusting your filters or search criteria.")
                }
            } else {
                "Record some wave data to see it here!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (hasActiveFilter) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onClearFilters) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Filters")
            }
        }
    }
}

/**
* Error view with retry option
*/
@Composable
fun ErrorView(
message: String,
onRetry: () -> Unit,
onBack: () -> Unit
) {
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Text(
        text = "Error",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row {
        Button(onClick = onRetry) {
            Text("Retry")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onBack) {
            Text("Go Back")
        }
    }
}
}

/**
 * Filter dropdown button
 */
@Composable
fun DropDownFilterButton(
    viewModel: HistoryViewModel,
    locationViewModel: LocationViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    // Get filter state from UIState
    val uiState by viewModel.uiState.collectAsState()
    val currentFilter = (uiState as? UiState.Success)?.data?.filterState
        ?: HistoryFilterState()

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Column(Modifier.padding(12.dp)) {
                HistoryFilterPanel(
                    historyViewModel = viewModel,
                    initialFilter = currentFilter,
                    locationViewModel = locationViewModel,
                    onApply = {
                        viewModel.updateFilter(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Individual history card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    record: HistoryRecord,
    isExpanded: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox in selection mode
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Timestamp and location
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.timestamp.toString(), // Format as needed
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = record.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expand/collapse button (not in selection mode)
                if (!isSelectionMode) {
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (isExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // Calculate averages
                val avgHeight = record.dataPoints.map { it.height }.average()
                val avgPeriod = record.dataPoints.map { it.period }.average()
                val avgDirection = record.dataPoints.map { it.direction }.average()

                // Display averages
                Text("Avg. Height: ${avgHeight.toDecimalString(1)} ft")
                Text("Avg. Period: ${avgPeriod.toDecimalString(1)} s")
                Text("Avg. Direction: ${avgDirection.toDecimalString(1)}°")

                Spacer(modifier = Modifier.height(12.dp))

                // Convert to MeasuredWaveData for graph
                val sessionDataPoints = record.dataPoints.map {
                    MeasuredWaveData(
                        waveHeight = it.height,
                        wavePeriod = it.period,
                        waveDirection = it.direction,
                        time = it.time
                    )
                }

                // Display graph
                HistoryGraph(
                    waveData = sessionDataPoints,
                    isInteractive = false,
                    isXLabeled = false
                )
            }
        }
    }
}

/**
 * Active filter info card
 */
@Composable
fun ActiveFilterInfo(filter: HistoryFilterState) {
    if (filter.searchLatLng != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Filtering near: ${filter.locationQuery}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Within ${filter.radiusMiles.toInt()} miles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
