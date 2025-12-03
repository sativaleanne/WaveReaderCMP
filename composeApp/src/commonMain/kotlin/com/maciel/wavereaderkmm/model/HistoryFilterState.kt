package com.maciel.wavereaderkmm.model


enum class SortOrder {
    DATE_ASCENDING,
    DATE_DESCENDING
}

data class HistoryFilterState(
    val locationQuery: String = "",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESCENDING
)