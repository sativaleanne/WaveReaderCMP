package com.maciel.wavereaderkmm.data

import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.model.WaveDataPoint
import com.maciel.wavereaderkmm.platform.*
import com.maciel.wavereaderkmm.utils.formatDateTime
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FirestoreRepository {

    actual suspend fun saveSession(
        measuredData: List<MeasuredWaveData>,
        locationName: String,
        latLng: Pair<Double, Double>?
    ): Result<String> {
        val userId = firebaseGetCurrentUserId()
            ?: return Result.failure(Exception("No user logged in"))

        if (measuredData.isEmpty()) {
            return Result.failure(Exception("No data to save"))
        }

        return suspendCancellableCoroutine { continuation ->
            val dataPoints = measuredData.map { data ->
                mapOf(
                    "time" to data.time,
                    "height" to data.waveHeight,
                    "period" to data.wavePeriod,
                    "direction" to data.waveDirection
                )
            }

            firestoreSaveSession(
                userId,
                locationName,
                latLng?.first ?: 0.0,
                latLng?.second ?: 0.0,
                dataPoints
            ) { result ->
                if (result.success) {
                    continuation.resume(Result.success(result.documentId ?: "unknown"))
                } else {
                    continuation.resume(Result.failure(Exception(result.error ?: "Unknown error")))
                }
            }
        }
    }

    actual suspend fun fetchHistoryRecords(
        locationQuery: String,
        sortDescending: Boolean,
        startDateMillis: Long?,
        endDateMillis: Long?
    ): List<HistoryRecord> {
        val userId = firebaseGetCurrentUserId() ?: return emptyList()

        return suspendCancellableCoroutine { continuation ->
            firestoreFetchHistory(userId, sortDescending, startDateMillis, endDateMillis) { sessions, error ->
                if (error != null) {
                    println("Error fetching history: $error")
                    continuation.resume(emptyList())
                } else if (sessions != null) {
                    val records = sessions.mapNotNull { session ->
                        try {
                            val id = session["id"] as? String ?: return@mapNotNull null
                            val location = session["location"] as? String ?: return@mapNotNull null

                            // Filter by location
                            if (locationQuery.isNotBlank() &&
                                !location.contains(locationQuery, ignoreCase = true)) {
                                return@mapNotNull null
                            }
                            AppLogger.i("FirestoreRepository", "session: $session")
                            val timestamp = session["timestamp"] as? Long ?: return@mapNotNull null
                            val dataPointsRaw = session["dataPoints"] as? List<Map<String, Any>> ?: emptyList()

                            val dataPoints = dataPointsRaw.map { point ->
                                WaveDataPoint(
                                    time = (point["time"] as? Number)?.toFloat() ?: 0f,
                                    height = (point["height"] as? Number)?.toFloat() ?: 0f,
                                    period = (point["period"] as? Number)?.toFloat() ?: 0f,
                                    direction = (point["direction"] as? Number)?.toFloat() ?: 0f
                                )
                            }

                            val formattedTimestamp = formatDateTime(timestamp)

                            HistoryRecord(
                                id = id,
                                timestamp = formattedTimestamp,
                                location = location,
                                lat = session["lat"] as? Double,
                                lon = session["lon"] as? Double,
                                dataPoints = dataPoints
                            )

                        } catch (e: Exception) {
                            println("Error parsing session: $e")
                            null
                        }
                    }
                    continuation.resume(records)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
    }

    actual suspend fun deleteHistoryRecord(recordId: String): Result<Unit> {
        val userId = firebaseGetCurrentUserId()
            ?: return Result.failure(Exception("No user logged in"))

        return suspendCancellableCoroutine { continuation ->
            firestoreDeleteSession(userId, recordId) { result ->
                if (result.success) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception(result.error ?: "Unknown error")))
                }
            }
        }
    }
}

