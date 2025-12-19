package com.maciel.wavereaderkmm.ui.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciel.wavereaderkmm.utils.toDecimalString

/**
 * Graph Painter
 */
object GraphPainter {

    /**
     * Draw horizontal grid lines
     */
    fun DrawScope.drawGridLines() {
        val gridLines = 8
        val yStep = size.height / gridLines
        val barWidthPx = 0.5.dp.toPx()

        // Top line
        drawLine(
            Color.Gray,
            Offset(0f, 0f),
            Offset(size.width, 0f),
            strokeWidth = barWidthPx
        )

        // Grid lines
        repeat(gridLines) { i ->
            val y = yStep * (i + 1)
            drawLine(
                Color.Gray,
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = barWidthPx
            )
        }
    }

    /**
     * Draw Y-axis labels
     *
     * @param textMeasurer For measuring and drawing text
     * @param maxValues Maximum values for each data series
     * @param units Units for each data series
     */
    fun DrawScope.drawYLabels(
        textMeasurer: TextMeasurer,
        maxValues: List<Float>,
        units: List<String>
    ) {
        val positions = units.indices.map { i -> size.width - 160f + i * 60f }
        val yStep = size.height / 6
        val labelPadding = 2.dp.toPx()

        val textStyle = TextStyle(
            fontSize = 10.sp,
            color = Color.Black
        )

        for (i in 0..6) {
            val y = size.height - (yStep * i)

            maxValues.forEachIndexed { index, maxVal ->
                val labelValue = (maxVal / 6f * i)
                val text = labelValue.toDecimalString(1) + units[index]

                // Measure text to get size
                val textLayoutResult = textMeasurer.measure(
                    text = text,
                    style = textStyle
                )

                // Draw text
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = positions[index],
                        y = y - labelPadding - textLayoutResult.size.height
                    )
                )
            }
        }
    }

    /**
     * Plot data lines on the graph
     */
    fun DrawScope.plotLines(
        dataSets: List<List<Float>>,
        maxValues: List<Float>,
        colors: List<Color>,
        selectedIndex: Int,
        pointSpacing: Float,
        graphHeight: Float
    ) {
        dataSets.forEachIndexed { index, data ->
            if (data.isNotEmpty()) {
                // Normalize data to graph height
                val normalized = data.map {
                    (it / (maxValues[index] + 0.01f)) * graphHeight
                }

                // Draw lines connecting points
                for (i in 1 until normalized.size) {
                    drawLine(
                        color = colors[index],
                        start = Offset(
                            (i - 1) * pointSpacing,
                            graphHeight - normalized[i - 1]
                        ),
                        end = Offset(
                            i * pointSpacing,
                            graphHeight - normalized[i]
                        ),
                        strokeWidth = 4f
                    )
                }

                // Draw circle at selected point
                if (selectedIndex in data.indices) {
                    drawCircle(
                        color = colors[index],
                        radius = 8f,
                        center = Offset(
                            selectedIndex * pointSpacing,
                            graphHeight - normalized[selectedIndex]
                        )
                    )
                }
            }
        }
    }

    /**
     * Draw X-axis labels (time labels)
     *
     * @param textMeasurer For measuring and drawing text
     * @param timeLabels Labels to display
     * @param pointSpacing Spacing between data points
     */
    fun DrawScope.drawXLabels(
        textMeasurer: TextMeasurer,
        timeLabels: List<String>,
        pointSpacing: Float
    ) {
        val labelPadding = 12.dp.toPx()

        val textStyle = TextStyle(
            fontSize = 12.sp,
            color = Color.Black
        )

        timeLabels.forEachIndexed { index, label ->
            // Only draw every other label to avoid crowding
            if (index % 2 == 0) {
                // Measure text
                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = textStyle
                )

                // Draw text centered at x position
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = index * pointSpacing - (textLayoutResult.size.width / 2),
                        y = size.height + labelPadding
                    )
                )
            }
        }
    }

    /**
     * Draw vertical line at selected point
     */
    fun DrawScope.drawCoordinate(
        selectedIndex: Int,
        totalPoints: Int,
        pointSpacing: Float
    ) {
        if (selectedIndex != -1) {
            val x = selectedIndex * pointSpacing
            drawLine(
                Color.Red,
                Offset(x, 0f),
                Offset(x, size.height),
                strokeWidth = 2f
            )
        }
    }
}
