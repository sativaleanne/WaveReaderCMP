package com.maciel.wavereaderkmm.ui.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.maciel.wavereaderkmm.model.Hourly
import com.maciel.wavereaderkmm.utils.formatTime

/**
 * Service Graph setup for API wave data
 */
@Composable
fun ServiceGraph(waveData: Hourly) {
    // Format time labels (e.g., "14" for 2pm)
    val timeLabels = waveData.time.map { formatTime(it) }

    // Create graph lines for all available data
    val lines = listOfNotNull(
        waveData.waveHeight?.let {
            GraphLine(it.filterNotNull(), "Wave Height", Color.Blue, "m")
        },
        waveData.wavePeriod?.let {
            GraphLine(it.filterNotNull(), "Wave Period", Color(0xFF008a8a), "s")
        },
        waveData.waveDirection?.let {
            GraphLine(it.filterNotNull(), "Wave Direction", Color(0xFF288041), "°")
        },
        waveData.windWaveHeight?.let {
            GraphLine(it.filterNotNull(), "Wind Height", Color.Red, "m")
        },
        waveData.windWavePeriod?.let {
            GraphLine(it.filterNotNull(), "Wind Period", Color.Magenta, "s")
        },
        waveData.windWaveDirection?.let {
            GraphLine(it.filterNotNull(), "Wind Direction", Color.Black, "°")
        },
        waveData.swellWaveHeight?.let {
            GraphLine(it.filterNotNull(), "Swell Height", Color.Yellow, "m")
        },
        waveData.swellWavePeriod?.let {
            GraphLine(it.filterNotNull(), "Swell Period", Color.LightGray, "s")
        },
        waveData.swellWaveDirection?.let {
            GraphLine(it.filterNotNull(), "Swell Direction", Color.DarkGray, "°")
        }
    )

    Graph(
        lines = lines,
        timeLabels = timeLabels,
        isInteractive = true
    )
}
