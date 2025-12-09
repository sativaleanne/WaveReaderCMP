package com.maciel.wavereaderkmm.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.model.WaveDataPoint
import com.maciel.wavereaderkmm.utils.formatDateTime
import kotlinx.coroutines.tasks.await

actual class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    actual suspend fun saveSession(
        measuredData: List<MeasuredWaveData>,
        locationName: String,
        latLng: Pair<Double, Double>?
    ): Result<String> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(Exception("No user logged in"))

        if (measuredData.isEmpty()) {
            return Result.failure(Exception("No data to save"))
        }

        return try {
            val sessionData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "location" to locationName,
                "dataPoints" to measuredData.map { data ->
                    mapOf(
                        "time" to data.time,
                        "height" to data.waveHeight,
                        "period" to data.wavePeriod,
                        "direction" to data.waveDirection
                    )
                }
            )

            latLng?.let { (lat, lon) ->
                sessionData["lat"] = lat
                sessionData["lon"] = lon
            }

            val docRef = firestore
                .collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .add(sessionData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun fetchHistoryRecords(
        locationQuery: String,
        sortDescending: Boolean,
        startDateMillis: Long?,
        endDateMillis: Long?
    ): List<HistoryRecord> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            var query = firestore
                .collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .orderBy("timestamp", if (sortDescending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)

            // Apply date filters
            if (startDateMillis != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", startDateMillis)
            }
            if (endDateMillis != null) {
                query = query.whereLessThanOrEqualTo("timestamp", endDateMillis)
            }

            val snapshot = query.get().await()

            snapshot.documents.mapNotNull { document ->
                try {
                    val location = document.getString("location") ?: return@mapNotNull null

                    // Filter by location
                    if (locationQuery.isNotBlank() &&
                        !location.contains(locationQuery, ignoreCase = true)) {
                        return@mapNotNull null
                    }

                    val dataPoints = (document.get("dataPoints") as? List<Map<String, Any>>)?.map { point ->
                        WaveDataPoint(
                            time = (point["time"] as Number).toFloat(),
                            height = (point["height"] as Number).toFloat(),
                            period = (point["period"] as Number).toFloat(),
                            direction = (point["direction"] as Number).toFloat()
                        )
                    } ?: emptyList()

                    val timestampMillis = document.getLong("timestamp") ?: return@mapNotNull null

                    HistoryRecord(
                        id = document.id,
                        timestamp = formatDateTime(timestampMillis),
                        location = location,
                        lat = document.getDouble("lat"),
                        lon = document.getDouble("lon"),
                        dataPoints = dataPoints
                    )
                } catch (e: Exception) {
                    println("Error parsing document: $e")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching history: $e")
            emptyList()
        }
    }

    actual suspend fun deleteHistoryRecord(recordId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(Exception("No user logged in"))

        return try {
            firestore
                .collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .document(recordId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}