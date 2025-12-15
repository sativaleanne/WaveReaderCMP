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
        println("DEBUG: Applying filters")
        println("  - Search coordinates: ${filter.searchLatLng}")
        println("  - Radius: ${filter.radiusMiles} miles")
        println("  - Location query (display): ${filter.locationQuery}")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val records = firestoreRepository.fetchHistoryRecords(
                locationQuery = "",
                sortDescending = filter.sortOrder == SortOrder.DATE_DESCENDING,
                startDateMillis = filter.startDateMillis,
                endDateMillis = filter.endDateMillis
            )

            println("DEBUG: Fetched ${records.size} raw records from Firestore")

            // Apply location filtering based on coordinates and proximity
            val filtered = if (filter.searchLatLng != null) {
                records.filter { record ->
                    if (record.lat != null && record.lon != null) {
                        val distance = calculateDistance(
                            filter.searchLatLng.first,
                            filter.searchLatLng.second,
                            record.lat,
                            record.lon
                        )
                        val withinRadius = distance <= filter.radiusMiles

                        println("DEBUG: Record '${record.location}' at (${record.lat}, ${record.lon})")
                        println("       Distance: ${distance.toInt()} miles - ${if (withinRadius) "INCLUDED" else "EXCLUDED"}")

                        withinRadius
                    } else {
                        println("DEBUG: Record '${record.location}' has no coordinates - EXCLUDED")
                        false
                    }
                }
            } else {
                println("DEBUG: No location filter applied - showing all records")
                records
            }

            println("DEBUG: Final filtered count: ${filtered.size} records")

            _historyData.value = filtered
            _isLoading.value = false
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * Returns distance in miles
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMiles = 3959.0

        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMiles * c
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