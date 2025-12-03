package com.maciel.wavereaderkmm.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciel.wavereaderkmm.data.FirestoreRepository
import com.maciel.wavereaderkmm.model.HistoryFilterState
import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HistoryViewModel
 *
 * Manages wave session history using GitLive Firebase SDK
 */
class HistoryViewModel(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    // History data
    private val _historyData = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val historyData: StateFlow<List<HistoryRecord>> = _historyData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Expanded items (for accordion-style UI)
    private val _expandedItems = MutableStateFlow<Set<String>>(emptySet())
    val expandedItems: StateFlow<Set<String>> = _expandedItems.asStateFlow()

    // Selection mode (for multi-select delete)
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems = _selectedItems.asStateFlow()

    // Filter state
    private val _filterState = MutableStateFlow(HistoryFilterState())
    val filterState: StateFlow<HistoryFilterState> = _filterState.asStateFlow()

    /**
     * Enable selection mode and select the first item
     */
    fun enableSelectionMode(itemId: String) {
        _isSelectionMode.value = true
        _selectedItems.value = setOf(itemId)
    }

    /**
     * Toggle item selection
     */
    fun toggleItemSelection(itemId: String) {
        _selectedItems.value = if (_selectedItems.value.contains(itemId)) {
            _selectedItems.value - itemId
        } else {
            _selectedItems.value + itemId
        }
    }

    /**
     * Clear selection and exit selection mode
     */
    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    /**
     * Delete selected items
     */
    fun deleteSelectedItems() {
        val itemsToDelete = _selectedItems.value.toSet()

        // Optimistically update UI
        _historyData.value = _historyData.value.filterNot {
            itemsToDelete.contains(it.id)
        }
        clearSelection()

        // Delete from Firestore in background
        viewModelScope.launch {
            var hasError = false

            for (id in itemsToDelete) {
                firestoreRepository.deleteHistoryRecord(id)
                    .onFailure { error ->
                        hasError = true
                        _errorMessage.value = "Delete failed: ${error.message}"
                    }
            }

            // If any deletion failed, refresh to restore correct state
            if (hasError) {
                applyFilters()
            }
        }
    }

    /**
     * Update filter and refresh data
     */
    fun updateFilter(newFilter: HistoryFilterState) {
        _filterState.value = newFilter
        applyFilters()
    }

    /**
     * Apply current filters and fetch data from Firestore
     */
    private fun applyFilters() {
        val filter = _filterState.value
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val records = firestoreRepository.fetchHistoryRecords(
                locationQuery = filter.locationQuery,
                sortDescending = filter.sortOrder == SortOrder.DATE_DESCENDING,
                startDateMillis = filter.startDateMillis,
                endDateMillis = filter.endDateMillis
            )

            // Additional client-side filtering (if needed)
            val filtered = if (filter.locationQuery.isNotBlank()) {
                records.filter {
                    it.location.contains(filter.locationQuery, ignoreCase = true)
                }
            } else {
                records
            }

            _historyData.value = filtered
            _isLoading.value = false
        }
    }

    /**
     * Toggle item expansion
     */
    fun toggleItemExpansion(id: String) {
        _expandedItems.value = if (_expandedItems.value.contains(id)) {
            _expandedItems.value - id
        } else {
            _expandedItems.value + id
        }
    }

    /**
     * Set default filter
     */
    fun setDefaultRecentFilter() {
        val defaultFilter = HistoryFilterState(
            locationQuery = "",
            sortOrder = SortOrder.DATE_DESCENDING,
            startDateMillis = null,
            endDateMillis = null
        )
        updateFilter(defaultFilter)
    }

    /**
     * Refresh data
     */
    fun refresh() {
        applyFilters()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}