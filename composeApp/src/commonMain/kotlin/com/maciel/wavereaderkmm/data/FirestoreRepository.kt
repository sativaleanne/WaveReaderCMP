package com.maciel.wavereaderkmm.data

import com.maciel.wavereaderkmm.model.HistoryRecord
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.model.WaveDataPoint
import com.maciel.wavereaderkmm.utils.currentTimeMillis
import com.maciel.wavereaderkmm.utils.formatDateTime
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

/**
 * Firestore Repository
 * Uses GitLive Firebase SDK for multiplatform support
 */
class FirestoreRepository {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    /**
     * Save wave session to Firestore
     */
    suspend fun saveSession(
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
            val session = FirestoreSession(
                timestamp = currentTimeMillis(),
                location = locationName,
                lat = latLng?.first,
                lon = latLng?.second,
                dataPoints = measuredData.map { data ->
                    WaveDataPoint(
                        time = data.time,
                        height = data.waveHeight,
                        period = data.wavePeriod,
                        direction = data.waveDirection
                    )
                }
            )

            val docRef = firestore
                .collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .add(session)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch history records from Firestore
     */
    suspend fun fetchHistoryRecords(
        locationQuery: String = "",
        sortDescending: Boolean = true,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): List<HistoryRecord> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            var query = firestore
                .collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .orderBy("timestamp", if (sortDescending) Direction.DESCENDING else Direction.ASCENDING)

            // Apply date filters
            if (startDateMillis != null) {
                query = query.where {
                    "timestamp" greaterThanOrEqualTo startDateMillis
                }
            }
            if (endDateMillis != null) {
                query = query.where {
                    "timestamp" lessThanOrEqualTo endDateMillis
                }
            }

            val snapshot = query.get()

            snapshot.documents.mapNotNull { document ->
                try {
                    val session = document.data<FirestoreSession>()

                    // Filter by location
                    if (locationQuery.isNotBlank() &&
                        !session.location.contains(locationQuery, ignoreCase = true)) {
                        return@mapNotNull null
                    }

                    HistoryRecord(
                        id = document.id,
                        timestamp = formatDateTime(session.timestamp),
                        location = session.location,
                        lat = session.lat,
                        lon = session.lon,
                        dataPoints = session.dataPoints
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

    /**
     * Delete a history record
     */
    suspend fun deleteHistoryRecord(recordId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(Exception("No user logged in"))

        return try {
            firestore
                .collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .document(recordId)
                .delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatTimestamp(millis: Long): String {
        // Simple formatting for now
        //datetime later
        return "Timestamp: $millis"
    }
}

@Serializable
data class FirestoreSession(
    val timestamp: Long,
    val location: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val dataPoints: List<WaveDataPoint>
)