package com.maciel.wavereaderkmm.ui.graph

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.maciel.wavereaderkmm.model.GraphDisplayOptions
import com.maciel.wavereaderkmm.model.MeasuredWaveData
import com.maciel.wavereaderkmm.utils.predictNextBigWave
import com.maciel.wavereaderkmm.utils.toDecimalString

/**
 * Sensor Graph setup
 */
@Composable
fun SensorGraph(
    waveData: List<MeasuredWaveData>,
    display: GraphDisplayOptions
) {
    if (waveData.isEmpty()) return

    // Filtered data based on display options
    val height = if (display.showHeight) waveData.map { it.waveHeight } else emptyList()
    val period = if (display.showPeriod) waveData.map { it.wavePeriod } else emptyList()
    val direction = if (display.showDirection) waveData.map { it.waveDirection } else emptyList()

    val timeValues = waveData.map { it.time }.toMutableList()

    // Determine forecast index
    val forecastIndex = if (display.showForecast && predictNextBigWave(waveData)) {
        waveData.lastIndex
    } else {
        -1
    }

    // Limit X axis labels to 10 to avoid crowding
    val maxLabels = 10
    val labelStep = if (timeValues.size > maxLabels) {
        timeValues.size / maxLabels
    } else {
        1
    }

    val timeLabels = timeValues.mapIndexed { index, time ->
        if (index % labelStep == 0 || index == timeValues.lastIndex) {
            "${time.toDecimalString(1)} s"
        } else {
            ""
        }
    }

    val mainLines = listOfNotNull(
        if (height.isNotEmpty()) {
            GraphLine(height, "Wave Height", Color.Blue, "ft")
        } else null,
        if (period.isNotEmpty()) {
            GraphLine(period, "Wave Period", Color(0xFF008a8a), "s")
        } else null,
        if (direction.isNotEmpty()) {
            GraphLine(direction, "Wave Direction", Color(0xFF288041), "°")
        } else null
    )

    Graph(
        lines = mainLines,
        timeLabels = timeLabels,
        isInteractive = true,
        isScrollable = true,
        forecastIndex = forecastIndex
    )
}
