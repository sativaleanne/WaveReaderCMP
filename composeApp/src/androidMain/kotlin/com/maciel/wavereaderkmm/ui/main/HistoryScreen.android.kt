package com.maciel.wavereaderkmm.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.platform.exportToCsvWithUri
import com.maciel.wavereaderkmm.platform.exportToJsonWithUri
import com.maciel.wavereaderkmm.platform.generateExportFilename
import com.maciel.wavereaderkmm.platform.rememberExportLauncher
import com.maciel.wavereaderkmm.ui.components.SmartExportDialog
import com.maciel.wavereaderkmm.ui.components.SnackbarHelper
import com.maciel.wavereaderkmm.viewmodels.HistoryViewModel
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.UiState

/**
 * ANDROID-SPECIFIC HistoryScreen Implementation
 *
 * Changes from common main:
 * - Uses rememberExportLauncher() to create file pickers
 * - Passes launcher-based callbacks to SmartExportDialog
 * - Uses exportToCsvWithUri() / exportToJsonWithUri() for actual export
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun HistoryScreen(
    viewModel: HistoryViewModel,
    locationViewModel: LocationViewModel,
    onBack: () -> Unit
) {

    println("HistoryScreen recomposed")

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Export dialog state
    var showExportDialog by remember { mutableStateOf(false) }

    // State to track what we're exporting
    var pendingExportData by remember { mutableStateOf<List<com.maciel.wavereaderkmm.model.HistoryRecord>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHelper(snackbarHostState, scope) }

    // Create SAF file picker launchers
    val exportLauncher = rememberExportLauncher(
        onCsvExport = { uri ->
            pendingExportData?.let { data ->
                exportToCsvWithUri(
                    context = context,
                    uri = uri,
                    data = data,
                    onSuccess = { msg -> snackbar.showSuccess(msg) },
                    onFailure = { err -> snackbar.showError(err) }
                )
            }
            pendingExportData = null
        },
        onJsonExport = { uri ->
            pendingExportData?.let { data ->
                exportToJsonWithUri(
                    context = context,
                    uri = uri,
                    data = data,
                    onSuccess = { msg -> snackbar.showSuccess(msg) },
                    onFailure = { err -> snackbar.showError(err) }
                )
            }
            pendingExportData = null
        }
    )

    Scaffold(
        topBar = {
            if (uiState is UiState.Success && !(uiState as UiState.Success).data.isSelectionMode){
                TopAppBar(
                    title = { Text("History") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        // Only show filter button when we have data
                        DropDownFilterButton(viewModel, locationViewModel)
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export all visible records"
                            )
                        }
                    }
                )
            }
            // Selection mode top app bar (shown when IN selection mode)
            if (uiState is UiState.Success && (uiState as UiState.Success).data.isSelectionMode) {
                val selectedCount = (uiState as UiState.Success).data.selectedItems.size
                TopAppBar(
                    title = { Text("$selectedCount selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }
                    },
                    actions = {
                        // Export selected button
                        IconButton(
                            onClick = { showExportDialog = true },
                            enabled = selectedCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export selected"
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteSelectedRecords() },
                            enabled = selectedCount > 0
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }

                        // Select all button
                        IconButton(onClick = { if (selectedCount > 0) {viewModel.deselectAll()} else {viewModel.selectAll()} }) {
                            if (selectedCount > 0) {
                                Icon(Icons.Default.Deselect, contentDescription = "Deselect all")
                            } else {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator()
                }

                is UiState.Success -> {
                    val historyState = state.data

                    // Show empty state if no records after filtering
                    if (historyState.filteredRecords.isEmpty()) {
                        EmptyHistoryView(
                            filterState = historyState.filterState,
                            onClearFilters = {
                                viewModel.setDefaultRecentFilter()
                                viewModel.refresh()
                            }
                        )
                    } else {
                        Column {
                            // Show active filter info
                            if (historyState.filterState.searchLatLng != null) {
                                ActiveFilterInfo(historyState.filterState)
                            }

                            // History list
                            HistoryList(
                                records = historyState.filteredRecords,
                                expandedItems = historyState.expandedItems,
                                isSelectionMode = historyState.isSelectionMode,
                                selectedItems = historyState.selectedItems,
                                onItemClick = { id -> viewModel.toggleItemExpansion(id) },
                                onItemSelect = { id -> viewModel.toggleItemSelection(id) },
                                onItemLongClick = { viewModel.toggleSelectionMode() }
                            )

                            // SMART EXPORT DIALOG with Android SAF integration
                            SmartExportDialog(
                                showDialog = showExportDialog,
                                isSelectionMode = historyState.isSelectionMode,
                                selectedCount = historyState.selectedItems.size,
                                allVisibleCount = historyState.filteredRecords.size,
                                onDismiss = { showExportDialog = false },

                                // trigger file pickers
                                onExportSelectedCsv = {
                                    val records = viewModel.getSelectedRecords()
                                    if (records.isNotEmpty()) {
                                        pendingExportData = records
                                        exportLauncher.launchCsvExport(
                                            generateExportFilename("wave_selected", "csv")
                                        )
                                    } else {
                                        snackbar.showError("No records selected")
                                    }
                                },

                                onExportSelectedJson = {
                                    val records = viewModel.getSelectedRecords()
                                    if (records.isNotEmpty()) {
                                        pendingExportData = records
                                        exportLauncher.launchJsonExport(
                                            generateExportFilename("wave_selected", "json")
                                        )
                                    } else {
                                        snackbar.showError("No records selected")
                                    }
                                },

                                onExportAllCsv = {
                                    val records = viewModel.getAllRecords()
                                    if (records.isNotEmpty()) {
                                        pendingExportData = records
                                        exportLauncher.launchCsvExport(
                                            generateExportFilename("wave_data", "csv")
                                        )
                                    } else {
                                        snackbar.showError("No records to export")
                                    }
                                },

                                onExportAllJson = {
                                    val records = viewModel.getAllRecords()
                                    if (records.isNotEmpty()) {
                                        pendingExportData = records
                                        exportLauncher.launchJsonExport(
                                            generateExportFilename("wave_data", "json")
                                        )
                                    } else {
                                        snackbar.showError("No records to export")
                                    }
                                }
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorView(
                        message = state.message ?: "Unknown error",
                        onRetry = { viewModel.clearError() },
                        onBack = onBack
                    )
                }
                is UiState.Empty -> {
                    EmptyHistoryView(
                        filterState = HistoryFilterState(),
                        onClearFilters = { viewModel.setDefaultRecentFilter()
                            viewModel.refresh()
                        }
                    )
                }
            }
        }
    }
}