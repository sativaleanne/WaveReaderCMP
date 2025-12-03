package com.maciel.wavereaderkmm.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.maciel.wavereaderkmm.model.MeasuredWaveData


data class GraphLine(
    val values: List<Float>,
    val label: String,
    val color: Color,
    val unit: String
)

/**
 * One Graph to rule them all
 */
@Composable
fun Graph(
    lines: List<GraphLine>,
    timeLabels: List<String>,
    isInteractive: Boolean = true,
    isScrollable: Boolean = false,
    isXLabeled: Boolean = true,
    forecastIndex: Int = -1
) {
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var userScrolling by remember { mutableStateOf(false) }

    val pointSpacing = 40f
    val dataPointCount = lines.firstOrNull()?.values?.size ?: 0
    val graphWidth = dataPointCount * pointSpacing

    val dataSets = lines.map { it.values }
    val maxValues = lines.map { it.values.maxOrNull() ?: 1f }
    val colors = lines.map { it.color }
    val units = lines.map { it.unit }

    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(dataPointCount, canvasWidth, userScrolling) {
        if (isScrollable && !userScrolling) {
            scrollOffset = (graphWidth - canvasWidth).coerceAtLeast(0f)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Graph Drawing Area
        Box(
            modifier = Modifier
                .height(300.dp)
                .clip(RectangleShape)
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .then(
                    if (isScrollable || isInteractive) Modifier.pointerInput(dataPointCount) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                userScrolling = true
                                if (isInteractive) {
                                    val x = offset.x + if (isScrollable) scrollOffset else 0f
                                    selectedIndex = (x / pointSpacing).toInt()
                                        .coerceIn(0, timeLabels.size - 1)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (isScrollable) {
                                    val maxScroll = (graphWidth - canvasSize.width)
                                        .coerceAtLeast(0f)
                                    scrollOffset = (scrollOffset - dragAmount.x)
                                        .coerceIn(0f, maxScroll)
                                }

                                if (isInteractive) {
                                    val x = change.position.x +
                                            if (isScrollable) scrollOffset else 0f
                                    selectedIndex = (x / pointSpacing).toInt()
                                        .coerceIn(0, timeLabels.size - 1)
                                }
                            },
                            onDragEnd = {
                                if (isInteractive) selectedIndex = -1
                                userScrolling = false
                            }
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        canvasWidth = it.width.toFloat()
                        canvasSize = it.toSize()
                    },
                onDraw = {
                    with(GraphPainter) {
                        drawGridLines()


                        drawYLabels(textMeasurer, maxValues, units)

                        if (isScrollable) drawContext.canvas.save()
                        drawContext.canvas.translate(
                            if (isScrollable) -scrollOffset else 0f,
                            0f
                        )

                        if (isXLabeled) {

                            drawXLabels(textMeasurer, timeLabels, pointSpacing)
                        }

                        plotLines(
                            dataSets,
                            maxValues,
                            colors,
                            selectedIndex,
                            pointSpacing,
                            size.height
                        )

                        drawCoordinate(selectedIndex, dataPointCount, pointSpacing)

                        if (forecastIndex != -1) {
                            drawForecastLine(forecastIndex, pointSpacing)
                        }

                        if (isScrollable) drawContext.canvas.restore()
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (selectedIndex != -1) {
            DrawCoordinateKey(selectedIndex, lines, timeLabels)
        }

        GraphLegend(lines)
    }
}

// Coordinate Input Selection
@Composable
fun DrawCoordinateKey(
    selectedIndex: Int,
    lines: List<GraphLine>,
    timeLabels: List<String>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        lines.forEach { line ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${line.label}:",
                    color = line.color,
                    fontSize = 12.sp
                )
                Text(
                    "${line.values.getOrNull(selectedIndex) ?: "-"} ${line.unit}",
                    color = line.color,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Displays Line and Color Legend
@Composable
fun GraphLegend(lines: List<GraphLine>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(line.color, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text(line.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun getLastTwenty(data: List<MeasuredWaveData>): List<MeasuredWaveData> {
    return if (data.size > 20) {
        data.takeLast(20)
    } else {
        data
    }
}
