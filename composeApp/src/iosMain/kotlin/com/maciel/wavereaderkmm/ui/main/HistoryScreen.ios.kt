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
import androidx.compose.material3.SnackbarHost
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
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.platform.exportToCsv
import com.maciel.wavereaderkmm.platform.exportToJson

import com.maciel.wavereaderkmm.ui.components.SmartExportDialog
import com.maciel.wavereaderkmm.ui.components.SnackbarHelper
import com.maciel.wavereaderkmm.viewmodels.HistoryViewModel
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import com.maciel.wavereaderkmm.viewmodels.UiState
import kotlinx.coroutines.launch

/**
 * iOS-specific HistoryScreen implementation.
 *
 * Export flow (iOS vs Android):
 *   Android — SAF launcher → user picks destination first → write there.
 *   iOS     — write to app's Documents directory → share sheet → user decides
 *             where the file goes (Files, AirDrop, Mail, etc.).
 *
 * Because exportToCsv / exportToJson are suspend functions we need a
 * coroutine scope.  We launch from the Composition scope so the coroutine is
 * automatically cancelled if the screen leaves the composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun HistoryScreen(
    viewModel: HistoryViewModel,
    locationViewModel: LocationViewModel,
    onBack: () -> Unit
) {

    println("HistoryScreen recomposed")

    val uiState by viewModel.uiState.collectAsState()

    // ── Export dialog visibility ──────────────────────────────────────────────
    var showExportDialog by remember { mutableStateOf(false) }

    // ── Feedback (mirrors Android's snackbar approach) ────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val snackbar          = remember { SnackbarHelper(snackbarHostState, scope) }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: run an export suspend function, show feedback, open share sheet
    //
    // exportFn  — one of exportToCsv / exportToJson, partially applied with data
    // label     — used in the error message only
    //
    // Why a local helper instead of repeating the launch { } block four times?
    // DRY, but more importantly: if the share-sheet logic ever needs to change
    // (e.g. iPad popover source rect) there is exactly one place to update.
    // ─────────────────────────────────────────────────────────────────────────
    // shareFile is now called inside exportToCsv/exportToJson after the IO
    // write completes on the background thread — execution resumes on main,
    // so UIKit is always touched from the right thread. onSuccess here is
    // just for snackbar feedback.
    fun launchExport(
        label: String,
        exportFn: suspend (onSuccess: (String) -> Unit, onFailure: (String) -> Unit) -> Unit
    ) {
        scope.launch {
            exportFn(
                { _ ->
                    snackbar.showSuccess("$label export ready")
                },
                { error ->
                    snackbar.showError(error)
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState is UiState.Success && !(uiState as UiState.Success).data.isSelectionMode) {
                TopAppBar(
                    title = { Text("History") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
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

                        IconButton(onClick = {
                            if (selectedCount > 0) viewModel.deselectAll()
                            else viewModel.selectAll()
                        }) {
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
                is UiState.Loading -> CircularProgressIndicator()

                is UiState.Success -> {
                    val historyState = state.data

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
                            if (historyState.filterState.searchLatLng != null) {
                                ActiveFilterInfo(historyState.filterState)
                            }

                            HistoryList(
                                records = historyState.filteredRecords,
                                expandedItems = historyState.expandedItems,
                                isSelectionMode = historyState.isSelectionMode,
                                selectedItems = historyState.selectedItems,
                                onItemClick = { id -> viewModel.toggleItemExpansion(id) },
                                onItemSelect = { id -> viewModel.toggleItemSelection(id) },
                                onItemLongClick = { viewModel.toggleSelectionMode() }
                            )

                            // ── Export dialog ─────────────────────────────────────
                            // Callbacks follow the same shape as Android's, but
                            // instead of a SAF launcher they go straight into a
                            // coroutine that writes the file and opens a share sheet.
                            SmartExportDialog(
                                showDialog = showExportDialog,
                                isSelectionMode = historyState.isSelectionMode,
                                selectedCount = historyState.selectedItems.size,
                                allVisibleCount = historyState.filteredRecords.size,
                                onDismiss = { showExportDialog = false },

                                onExportSelectedCsv = {
                                    val records = viewModel.getSelectedRecords()
                                    if (records.isNotEmpty()) {
                                        launchExport("CSV") { onSuccess, onFailure ->
                                            exportToCsv(records, onSuccess, onFailure)
                                        }
                                    } else {
                                        snackbar.showError("No records selected")
                                    }
                                },

                                onExportSelectedJson = {
                                    val records = viewModel.getSelectedRecords()
                                    if (records.isNotEmpty()) {
                                        launchExport("JSON") { onSuccess, onFailure ->
                                            exportToJson(records, onSuccess, onFailure)
                                        }
                                    } else {
                                        snackbar.showError("No records selected")
                                    }
                                },

                                onExportAllCsv = {
                                    val records = viewModel.getAllRecords()
                                    if (records.isNotEmpty()) {
                                        launchExport("CSV") { onSuccess, onFailure ->
                                            exportToCsv(records, onSuccess, onFailure)
                                        }
                                    } else {
                                        snackbar.showError("No records to export")
                                    }
                                },

                                onExportAllJson = {
                                    val records = viewModel.getAllRecords()
                                    if (records.isNotEmpty()) {
                                        launchExport("JSON") { onSuccess, onFailure ->
                                            exportToJson(records, onSuccess, onFailure)
                                        }
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
                        onClearFilters = {
                            viewModel.setDefaultRecentFilter()
                            viewModel.refresh()
                        }
                    )
                }
            }
        }
    }
}