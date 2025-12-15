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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.ui.components.HistoryFilterPanel
import com.maciel.wavereaderkmm.ui.components.SnackbarHelper
import com.maciel.wavereaderkmm.ui.graph.HistoryGraph
import com.maciel.wavereaderkmm.utils.toDecimalString
import com.maciel.wavereaderkmm.viewmodels.HistoryViewModel
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel

/**
 * History Screen - View saved wave sessions
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    locationViewModel: LocationViewModel,
    onBack: () -> Unit
) {
    val historyData by viewModel.historyData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val expandedItems by viewModel.expandedItems.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHelper(snackbarHostState, scope) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Handle back button
    BackHandler {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else {
            onBack()
        }
    }

    // Load data on first launch
    LaunchedEffect(Unit) {
        viewModel.setDefaultRecentFilter()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selection mode top bar OR normal buttons
            if (isSelectionMode) {
                val selectedHistoryData = historyData.filter { it.id in selectedItems }
                SelectionTopBar(
                    selectedItems = selectedHistoryData,
                    selectedCount = selectedItems.size,
                    onCancel = { viewModel.clearSelection() },
                    onDelete = { showDeleteDialog = true },
                    onExport = { format ->
                        // Platform-specific export will be handled
                        snackbar.showInfo("Export to $format")
                    }
                )

                // Delete confirmation dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        title = { Text("Delete Selected Sessions") },
                        text = { Text("Are you sure you want to delete ${selectedItems.size} session(s)?") },
                        onDismissRequest = { showDeleteDialog = false },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.deleteSelectedItems()
                                showDeleteDialog = false
                                snackbar.showSuccess("Deleted ${selectedItems.size} session(s)")
                            }) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            } else {
                // Normal mode - Filter and Export buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DropDownFilterButton(viewModel, locationViewModel)
                    DropDownExportButton(
                        historyData = historyData,
                        onExport = { format ->
                            snackbar.showInfo("Export to $format (coming soon)")
                        }
                    )
                }
                ActiveFilterInfo(filter = viewModel.filterState.collectAsState().value)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content: Loading / Empty / List
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                historyData.isEmpty() -> {
                    // Show empty state
                    val currentFilter by viewModel.filterState.collectAsState()
                    EmptyHistoryState(
                        filterState = currentFilter,
                        onClearFilters = {
                            viewModel.updateFilter(HistoryFilterState())
                        }
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(
                            items = historyData,
                            key = { it.id }
                        ) { record ->
                            HistoryCard(
                                record = record,
                                isExpanded = expandedItems.contains(record.id),
                                onToggle = { viewModel.toggleItemExpansion(record.id) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItems.contains(record.id),
                                onLongClick = { viewModel.enableSelectionMode(record.id) },
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(record.id)
                                    } else {
                                        viewModel.toggleItemExpansion(record.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState(
    filterState: HistoryFilterState,
    onClearFilters: () -> Unit
) {
    // Determine if any filters are active
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
            imageVector = if (hasActiveFilter) Icons.Default.FilterAlt else Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasActiveFilter) "No matching records found" else "No history data yet",
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
 * Selection mode top bar
 */
@Composable
fun SelectionTopBar(
    selectedItems: List<HistoryRecord>,
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onExport: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel and count
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
            }
            Text("$selectedCount selected", style = MaterialTheme.typography.titleMedium)
        }

        // Export and Delete buttons
        Row {
            DropDownExportButton(
                historyData = selectedItems,
                onExport = onExport,
                trigger = { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                }
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

/**
 * Filter dropdown button
 */
@Composable
fun DropDownFilterButton(viewModel: HistoryViewModel, locationViewModel: LocationViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = !expanded },
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
        ) {
            Text("Filter")
            Icon(
                imageVector = if (!expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = "Expand"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Column(Modifier.padding(12.dp)) {
                HistoryFilterPanel(
                    initialFilter = viewModel.filterState.collectAsState().value,
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
 * Export dropdown button
 */
@Composable
fun DropDownExportButton(
    historyData: List<HistoryRecord>,
    onExport: (String) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit = { onClick ->
        Button(
            onClick = onClick,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
        ) {
            Text("Export")
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand")
        }
    }
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        trigger { expanded = !expanded }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export to JSON") },
                onClick = {
                    if (historyData.isNotEmpty()) {
                        onExport("JSON")
                    }
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Export to CSV") },
                onClick = {
                    if (historyData.isNotEmpty()) {
                        onExport("CSV")
                    }
                    expanded = false
                }
            )
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
    onToggle: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
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
                        text = record.timestamp,
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
                val avgPeriod = record.dataPoints.map { it.period }.average() * 100
                val avgDirection = record.dataPoints.map { it.direction }.average()

                // Display averages
                Text("Avg. Height: ${(avgHeight.toDecimalString(1))} ft")
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