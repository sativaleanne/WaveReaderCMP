package com.maciel.wavereaderkmm.data

import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData

actual class FirestoreRepository {

    actual suspend fun saveSession(
        measuredData: List<MeasuredWaveData>,
        locationName: String,
        latLng: Pair<Double, Double>?
    ): Result<String> {
        // TODO: Implement after iOS build
        return Result.failure(Exception("Not implemented yet"))
    }

    actual suspend fun fetchHistoryRecords(
        locationQuery: String,
        sortDescending: Boolean,
        startDateMillis: Long?,
        endDateMillis: Long?
    ): List<HistoryRecord> {
        // TODO: Implement after iOS build
        return emptyList()
    }

    actual suspend fun deleteHistoryRecord(recordId: String): Result<Unit> {
        // TODO: Implement after iOS build
        return Result.failure(Exception("Not implemented yet"))
    }
}