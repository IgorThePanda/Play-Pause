package com.igorthepadna.play_pause

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.yield
import kotlin.math.sin

@Composable
fun SquigglySlider(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Long = 0,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val density = LocalDensity.current
    val maxAmplitudePx = with(density) { 4.dp.toPx() }
    val thumbRadiusIdle = with(density) { 10.dp.toPx() }
    val thumbRadiusDragging = with(density) { 12.dp.toPx() }
    
    var isDragging by remember { mutableStateOf(false) }
    
    val thumbRadius by animateFloatAsState(
        targetValue = if (isDragging) thumbRadiusDragging else thumbRadiusIdle,
        label = "ThumbRadius"
    )
    
    var phase by remember { mutableStateOf(0f) }
    
    val currentAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) maxAmplitudePx else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow, 
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "SquiggleAmplitude"
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = withFrameNanos { it }
            while (true) {
                val currentTime = withFrameNanos { it }
                val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                phase += deltaTime * 5f 
                lastTime = currentTime
                yield()
            }
        }
    }

    // Optimization: Pre-calculate constants and reuse Path
    val segmentLength = 6f // Increased for better performance on chugging devices
    val frequency = 1 / 15f 
    val envelopeDistance = with(density) { 16.dp.toPx() }
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onDragStart()
                        onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                        change.consume()
                    },
                    onDragEnd = { 
                        isDragging = false 
                        onDragEnd()
                    },
                    onDragCancel = { 
                        isDragging = false 
                        onDragEnd()
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val activeWidth = width * value
        val strokeWidthPx = 4.dp.toPx()

        if (activeWidth < width) {
            drawLine(
                color = inactiveColor,
                start = Offset(activeWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        if (activeWidth > 0) {
            path.reset()
            path.moveTo(0f, centerY)
            
            var x = 0f
            while (x < activeWidth) {
                val startEnvelope = (x / envelopeDistance).coerceIn(0f, 1f)
                val endEnvelope = ((activeWidth - x) / envelopeDistance).coerceIn(0f, 1f)
                val combinedEnvelope = startEnvelope * endEnvelope
                
                val y = centerY + currentAmplitude * sin(x * frequency + phase) * combinedEnvelope
                path.lineTo(x, y)
                x += segmentLength
            }
            
            path.lineTo(activeWidth, centerY)

            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(activeWidth, centerY)
        )
    }
}
