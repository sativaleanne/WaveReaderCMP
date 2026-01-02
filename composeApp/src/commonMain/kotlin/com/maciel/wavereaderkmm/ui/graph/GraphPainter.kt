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
        val numSteps = 5
        val positions = units.indices.map { i -> size.width - 160f + i * 60f }
        val yStep = size.height / numSteps.toFloat()
        val labelPadding = 2.dp.toPx()
        val bgPadding = 1.dp.toPx()
        val horizontalSpacing = 3.dp.toPx()

        val textStyle = TextStyle(
            fontSize = 10.sp,
            color = Color.DarkGray
        )

        val rightMargin = 4.dp.toPx()

        for (i in 0..numSteps) {
            val y = size.height - (yStep * i)
            var currentX = size.width - labelPadding

            maxValues.indices.reversed().forEach { index ->
                if (index >= maxValues.size) return@forEach

                val labelValue = (maxValues[index] / numSteps.toFloat() * i)
                val text = "${labelValue.toDecimalString(1)}${units[index]}"

                // Measure text
                val measured = textMeasurer.measure(text, style = textStyle)
                val textWidth = measured.size.width.toFloat()
                val textHeight = measured.size.height.toFloat()

                // Calculate position (right-aligned from currentX)
                val x = currentX - textWidth - bgPadding * 2
                val textY = y - textHeight / 2  // Center vertically on grid line

                // Measure text to get size
                val textLayoutResult = textMeasurer.measure(
                    text = text,
                    style = textStyle
                )

                // Draw semi-transparent background for readability
                drawRect(
                    color = Color.White.copy(alpha = 0.8f),
                    topLeft = Offset(
                        x - bgPadding,
                        textY - bgPadding
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        textWidth + bgPadding * 2,
                        textHeight + bgPadding * 2
                    )
                )
                // Draw text
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(x, textY)
                )
                // Move X position left for next label
                currentX = x - horizontalSpacing
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
        val labelPadding = 4.dp.toPx()

        val textStyle = TextStyle(
            fontSize = 10.sp,
            color = Color.DarkGray
        )

        timeLabels.forEachIndexed { index, label ->
            if (label.isNotEmpty()) {
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
                            y = size.height - textLayoutResult.size.height - labelPadding
                        )
                    )
                }
            }
        }
    }

    /**
     * Draw Y-axis coordinates at selection
     */
    fun DrawScope.drawYCoordinates(
        textMeasurer: TextMeasurer,
        selectedIndex: Int,
        dataSets: List<List<Float>>,
        maxValues: List<Float>,
        colors: List<Color>,
        units: List<String>,
        graphHeight: Float
    ) {
        if (selectedIndex == -1) return

        val labelPadding = 1.dp.toPx()
        val bgPadding = 1.dp.toPx()
        val horizontalOffset = 12.dp.toPx()  // Offset from the point
        val collisionThreshold = 4.dp.toPx()  // Min vertical distance between labels

        val textStyle = TextStyle(
            fontSize = 10.sp,
            color = Color.Black  // White text on colored background
        )

        // Data structure to hold label information
        data class LabelInfo(
            val value: String,
            val color: Color,
            val normalizedY: Float,
            var displayY: Float,
            val textWidth: Float,
            val textHeight: Float
        )

        // Step 1: Collect all label information
        val labels = mutableListOf<LabelInfo>()

        dataSets.forEachIndexed { index, data ->
            if (selectedIndex in data.indices) {
                val value = data[selectedIndex]

                // Normalize Y position (same logic as plotLines)
                val normalizedY = (value / (maxValues[index] + 0.01f)) * graphHeight
                val displayY = graphHeight - normalizedY

                // Format the value text
                val text = "${value.toDecimalString(1)}${units[index]}"

                // Measure text dimensions
                val measured = textMeasurer.measure(text, style = textStyle)
                val textWidth = measured.size.width.toFloat()
                val textHeight = measured.size.height.toFloat()

                labels.add(
                    LabelInfo(
                        value = text,
                        color = colors[index],
                        normalizedY = normalizedY,
                        displayY = displayY,
                        textWidth = textWidth,
                        textHeight = textHeight
                    )
                )
            }
        }

        // Step 2: Sort labels by Y position (top to bottom)
        labels.sortBy { it.displayY }

        // Step 3: Collision detection and resolution
        // Move overlapping labels apart vertically
        for (i in 1 until labels.size) {
            val prev = labels[i - 1]
            val curr = labels[i]

            val prevBottom = prev.displayY + prev.textHeight + bgPadding * 2
            val currTop = curr.displayY - bgPadding

            if (prevBottom + collisionThreshold > currTop) {
                // Collision detected - shift current label down
                curr.displayY = prevBottom + collisionThreshold + bgPadding
            }
        }

        // Step 4: Draw labels
        var currentX = size.width - labelPadding

        labels.forEach { label ->
            // Determine horizontal position

            val x = currentX - label.textWidth - bgPadding * 2

            // Vertically center the label on its Y position
            val textY = label.displayY - (label.textHeight / 2)

            // Ensure label stays on canvas vertically
            val finalTextY = textY.coerceIn(
                0f,
                size.height - bgPadding * 2
            )

            // Draw the value text
            val textLayoutResult = textMeasurer.measure(
                text = label.value,
                style = textStyle
            )
            // draw white background
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(
                    x - bgPadding,
                    textY - bgPadding
                ),
                size = androidx.compose.ui.geometry.Size(
                    label.textWidth + bgPadding * 2,
                    label.textHeight + bgPadding * 2
                )
            )

            // Draw semi-transparent color
            drawRect(
                color = label.color.copy(alpha = 0.2f),
                topLeft = Offset(
                    x - bgPadding,
                    textY - bgPadding
                ),
                size = androidx.compose.ui.geometry.Size(
                    label.textWidth + bgPadding * 2,
                    label.textHeight + bgPadding * 2
                )
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x, finalTextY)
            )
            currentX = x - bgPadding
        }
    }

    /**
     * Draw X-axis coordinates at selection
     */
    fun DrawScope.drawXCoordinates(
        textMeasurer: TextMeasurer,
        selectedIndex: Int,
        timeLabels: List<String>,
        pointSpacing: Float
    ) {
        if (selectedIndex == -1 || selectedIndex !in timeLabels.indices) return

        val labelPadding = 1.dp.toPx()
        val bgPadding = 4.dp.toPx()
        val bottomMargin = 1.dp.toPx()  // Space from bottom edge

        val textStyle = TextStyle(
            fontSize = 10.sp,
            color = Color.Black
        )

        // Measure the text to get dimensions
        val textLayoutResult = textMeasurer.measure(
            text = timeLabels[selectedIndex],
            style = textStyle
        )

        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat()

        // Calculate centered X position for the selected point
        val centerX = selectedIndex * pointSpacing

        // Calculate the left edge of the text box
        var textX = centerX - (textWidth / 2)

        // label stays within canvas
        val boxWidth = textWidth + (bgPadding * 2)
        if (textX - bgPadding < 0f) {
            // Too far left, align to left edge
            textX = bgPadding
        } else if (textX + boxWidth + bgPadding > size.width) {
            // Too far right, align to right edge
            textX = size.width - boxWidth - bgPadding
        }

        // Position at bottom of canvas
        val textY = size.height - labelPadding

        // Draw the time label
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(textX, textY)
        )
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
