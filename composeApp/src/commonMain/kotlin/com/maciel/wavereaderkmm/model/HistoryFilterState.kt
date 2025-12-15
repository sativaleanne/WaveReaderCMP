package com.maciel.wavereaderkmm.model


enum class SortOrder {
    DATE_ASCENDING,
    DATE_DESCENDING
}

data class HistoryFilterState(
    val locationQuery: String = "", // For display only
    val searchLatLng: Pair<Double, Double>? = null, // Actual filter - always use coordinates
    val radiusMiles: Double = 25.0,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESCENDING
)