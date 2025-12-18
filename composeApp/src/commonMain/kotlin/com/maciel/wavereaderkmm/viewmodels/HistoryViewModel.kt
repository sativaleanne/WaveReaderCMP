package com.maciel.wavereaderkmm.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciel.wavereaderkmm.data.FirestoreRepository
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.SortOrder
import com.maciel.wavereaderkmm.utils.toRadians
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * History UI State
 */
data class HistoryUiState(
    val historyRecords: List<HistoryRecord> = emptyList(),
    val expandedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val filterState: HistoryFilterState = HistoryFilterState(
        sortOrder = SortOrder.DATE_DESCENDING,
        startDateMillis = null,
        endDateMillis = null
    )
)

/**
 * HistoryViewModel
 *
 * Manages wave session history using GitLive Firebase SDK
 */
class HistoryViewModel(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    // Single source of truth for all history state
    private val _uiState = MutableStateFlow<UiState<HistoryUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<HistoryUiState>> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Load history from Firestore
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val records = firestoreRepository.fetchHistoryRecords()
                _uiState.value = UiState.Success(
                    HistoryUiState(historyRecords = records)
                )
                // Apply initial filters after loading
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = "Failed to load history: ${e.message}",
                    exception = e
                )
            }
        }
    }

    /**
     * Update filter and reapply
     */
    fun updateFilter(newFilter: HistoryFilterState) {
        getCurrentState()?.let { currentState ->
            updateSuccessState(currentState.copy(filterState = newFilter))
            applyFilters()
        }
    }

    /**
     * Apply current filters to history records
     */
    private fun applyFilters() {
        val currentState = getCurrentState() ?: return
        val filter = currentState.filterState

        // Get all records
        val allRecords = currentState.historyRecords

        // Apply date filters
        var filtered = allRecords.filter { record ->
            val timestamp = record.timestampMillis
            val afterStart = filter.startDateMillis?.let { timestamp >= it } ?: true
            val beforeEnd = filter.endDateMillis?.let { timestamp <= it } ?: true
            afterStart && beforeEnd
        }

        // Apply location filter (if specified)
        filtered = if (filter.searchLatLng != null) {
            filtered.filter { record ->
                if (record.lat != null && record.lon != null) {
                    val distance = calculateDistance(
                        filter.searchLatLng.first,
                        filter.searchLatLng.second,
                        record.lat,
                        record.lon
                    )
                    distance <= filter.radiusMiles
                } else {
                    false
                }
            }
        } else {
            filtered
        }

        // Apply sorting
        filtered = when (filter.sortOrder) {
            SortOrder.DATE_ASCENDING -> filtered.sortedBy { it.timestamp }
            SortOrder.DATE_DESCENDING -> filtered.sortedByDescending { it.timestamp }
            SortOrder.LOCATION_NAME -> filtered.sortedBy { it.location }
        }

        // Update state with filtered records
        updateSuccessState(currentState.copy(historyRecords = filtered))
    }

    /**
     * Delete a single history record
     *
     * REASONING: Optimistic update pattern - immediately update UI,
     * then sync with backend
     */
    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            val currentState = getCurrentState() ?: return@launch

            try {
                // Optimistic update: remove from UI immediately
                val updatedRecords = currentState.historyRecords.filter { it.id != recordId }
                updateSuccessState(currentState.copy(historyRecords = updatedRecords))

                // Delete from backend
                firestoreRepository.deleteHistoryRecord(recordId)

            } catch (e: Exception) {
                // On error, reload to restore correct state
                _uiState.value = UiState.Error(
                    message = "Failed to delete: ${e.message}",
                    exception = e
                )
                loadHistory() // Reload to get correct state
            }
        }
    }

    /**
     * Delete multiple selected records
     *
     * REASONING: Batch operation - delete all selected items and clear selection mode.
     * This shows atomic state updates with multiple changes at once.
     */
    fun deleteSelectedRecords() {
        viewModelScope.launch {
            val currentState = getCurrentState() ?: return@launch
            val idsToDelete = currentState.selectedItems

            try {
                // Optimistic update
                val updatedRecords = currentState.historyRecords.filter {
                    it.id !in idsToDelete
                }
                updateSuccessState(
                    currentState.copy(
                        historyRecords = updatedRecords,
                        selectedItems = emptySet(),
                        isSelectionMode = false
                    )
                )

                // Delete from backend
                idsToDelete.forEach { id ->
                    firestoreRepository.deleteHistoryRecord(id)
                }

            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = "Failed to delete records: ${e.message}",
                    exception = e
                )
                loadHistory()
            }
        }
    }

    /**
     * Toggle item expansion
     */
    fun toggleItemExpansion(id: String) {
        getCurrentState()?.let { currentState ->
            val newExpanded = if (id in currentState.expandedItems) {
                currentState.expandedItems - id
            } else {
                currentState.expandedItems + id
            }
            updateSuccessState(currentState.copy(expandedItems = newExpanded))
        }
    }

    /**
     * Toggle selection mode on/off
     *
     * When turning off selection mode, also clear selected items.
     * This shows how to make related state changes atomically.
     */
    fun toggleSelectionMode() {
        getCurrentState()?.let { currentState ->
            val newMode = !currentState.isSelectionMode
            updateSuccessState(
                currentState.copy(
                    isSelectionMode = newMode,
                    selectedItems = if (newMode) currentState.selectedItems else emptySet()
                )
            )
        }
    }

    /**
     * Toggle item selection
     *
     * Add or remove from selection Set.
     * Sets are perfect for selection because they prevent duplicates.
     */
    fun toggleItemSelection(id: String) {
        getCurrentState()?.let { currentState ->
            val newSelection = if (id in currentState.selectedItems) {
                currentState.selectedItems - id
            } else {
                currentState.selectedItems + id
            }
            updateSuccessState(currentState.copy(selectedItems = newSelection))
        }
    }

    /**
     * Select all items
     */
    fun selectAll() {
        getCurrentState()?.let { currentState ->
            val allIds = currentState.historyRecords.map { it.id }.toSet()
            updateSuccessState(currentState.copy(selectedItems = allIds))
        }
    }

    /**
     * Deselect all items
     */
    fun deselectAll() {
        getCurrentState()?.let { currentState ->
            updateSuccessState(currentState.copy(selectedItems = emptySet()))
        }
    }

    /**
     * Set default recent filter
     */
    fun setDefaultRecentFilter() {
        val defaultFilter = HistoryFilterState(
            sortOrder = SortOrder.DATE_DESCENDING,
            startDateMillis = null,
            endDateMillis = null
        )
        updateFilter(defaultFilter)
    }

    /**
     * Refresh data from backend
     */
    fun refresh() {
        loadHistory()
    }

    /**
     * Clear error and retry
     */
    fun clearError() {
        loadHistory()
    }

    // --- Helper Methods ---

    /**
     * Safely get current HistoryUiState if in Success state
     *
     * REASONING: Many operations only make sense when we have data loaded.
     * This helper prevents null pointer exceptions and makes code cleaner.
     */
    private fun getCurrentState(): HistoryUiState? {
        return (_uiState.value as? UiState.Success)?.data
    }

    /**
     * Update state while preserving Success wrapper
     * keep the Success wrapper and just update the inner HistoryUiState.
     */
    private fun updateSuccessState(newState: HistoryUiState) {
        _uiState.value = UiState.Success(newState)
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * Returns distance in miles
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusMiles = 3959.0

        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMiles * c
    }

}