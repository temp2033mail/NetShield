package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SpeedSparkline(
    dataPoints: List<Float>,
    lineColor: Color,
    gradientStartColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (dataPoints.isEmpty()) return@Canvas

        val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(10f)
        val width = size.width
        val height = size.height

        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val normalized = (value / maxVal).coerceIn(0f, 1f)
            val y = height - (normalized * (height - 12f)) - 6f

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw gradient area under the curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStartColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw smooth line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
    }
}
