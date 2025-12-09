package com.maciel.wavereaderkmm.data

import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData

expect class FirestoreRepository() {
    suspend fun saveSession(
        measuredData: List<MeasuredWaveData>,
        locationName: String,
        latLng: Pair<Double, Double>?
    ): Result<String>

    suspend fun fetchHistoryRecords(
        locationQuery: String = "",
        sortDescending: Boolean = true,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): List<HistoryRecord>

    suspend fun deleteHistoryRecord(recordId: String): Result<Unit>
}