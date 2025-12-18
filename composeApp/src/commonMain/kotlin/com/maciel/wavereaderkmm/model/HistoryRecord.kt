package com.maciel.wavereaderkmm.model

import kotlinx.serialization.Serializable


data class HistoryRecord(
    val id: String,
    val timestamp: String,
    val timestampMillis: Long,
    val location: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val dataPoints: List<WaveDataPoint>
)


@Serializable
data class WaveDataPoint(
    val time: Float,
    val height: Float,
    val period: Float,
    val direction: Float
)
