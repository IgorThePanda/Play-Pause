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
    
    // Animate thumb radius when dragging
    val thumbRadius by animateFloatAsState(
        targetValue = if (isDragging) thumbRadiusDragging else thumbRadiusIdle,
        label = "ThumbRadius"
    )
    
    // Phase for the squiggle animation
    var phase by remember { mutableStateOf(0f) }
    
    // Animate amplitude based on isPlaying
    val currentAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) maxAmplitudePx else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow, 
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "SquiggleAmplitude"
    )

    // Update phase while playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = withFrameMillis { it }
            while (true) {
                val currentTime = withFrameMillis { it }
                val deltaTime = (currentTime - lastTime) / 1000f
                // Slowed down the waves further (from 6f to 5f)
                phase += deltaTime * 5f 
                lastTime = currentTime
                yield()
            }
        }
    }

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

        // 1. Draw inactive track
        if (activeWidth < width) {
            drawLine(
                color = inactiveColor,
                start = Offset(activeWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // 2. Draw active track
        if (activeWidth > 0) {
            val path = Path()
            val segmentLength = 2f // px per segment
            val frequency = 1 / 15f // wavelength control
            
            path.moveTo(0f, centerY)
            
            var x = 0f
            while (x < activeWidth) {
                // Envelope at the start and end of the active track to avoid artifacts
                val startEnvelope = (x / 16.dp.toPx()).coerceIn(0f, 1f)
                val endEnvelope = ((activeWidth - x) / 16.dp.toPx()).coerceIn(0f, 1f)
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

        // 3. Draw thumb
        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(activeWidth, centerY)
        )
    }
}
