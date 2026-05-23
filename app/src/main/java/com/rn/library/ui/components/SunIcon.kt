package com.rn.library.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SunIcon(
    onClick: () -> Unit,
    color: Color,
    iconSize: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val yellowColor = Color(0xFFFFD700) // Yellow color
    val interactionSource = remember { MutableInteractionSource() }
    Canvas(
        modifier = modifier
            .size(iconSize)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            )
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val minDimension = size.minDimension
        val radius = minDimension / 3
        val rayLength = minDimension / 6
        val rayCount = 14
        
        // Draw rays
        for (i in 0 until rayCount) {
            val angle = (i * 360f / rayCount) * (Math.PI / 180f).toFloat()
            val startX = centerX + radius * kotlin.math.cos(angle)
            val startY = centerY + radius * kotlin.math.sin(angle)
            val endX = centerX + (radius + rayLength) * kotlin.math.cos(angle)
            val endY = centerY + (radius + rayLength) * kotlin.math.sin(angle)
            
            drawLine(
                color = yellowColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = minDimension / 24,
                cap = StrokeCap.Round
            )
        }
        
        // Draw circle
        drawCircle(
            color = yellowColor,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = minDimension / 20)
        )
    }
}
