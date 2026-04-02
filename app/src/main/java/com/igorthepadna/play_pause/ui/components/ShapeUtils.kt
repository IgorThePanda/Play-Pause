package com.igorthepadna.play_pause.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MorphingButtonShape(private val progress: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = size.minDimension / 2f
        
        // Increased points for even smoother curves
        val points = 72 
        val angleStep = (2 * PI / points).toFloat()
        
        for (i in 0 until points) {
            val angle = i * angleStep
            
            // Smoother Scalloped/Cookie radius (progress = 1)
            // Reduced amplitude (0.06f) to make tips less sharp
            val lobes = 12
            val cookieWave = cos(angle * lobes)
            val cookieRadius = baseRadius * (0.94f + 0.06f * cookieWave)
            
            // Rounder Squircle radius (progress = 0)
            // Power 2.4 is closer to a circle (power 2.0) than before (power 3.0)
            val cosA = cos(angle)
            val sinA = sin(angle)
            val squirclePower = 2.4
            val squircleRadius = baseRadius * (1f / Math.pow((Math.pow(Math.abs(cosA).toDouble(), squirclePower) + Math.pow(Math.abs(sinA).toDouble(), squirclePower)), 1.0/squirclePower)).toFloat()
            val finalSquircleRadius = squircleRadius * 0.9f

            // Interpolate radius
            val radius = finalSquircleRadius * (1f - progress) + cookieRadius * progress
            
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevAngle = (i - 1) * angleStep
                val midAngle = (prevAngle + angle) / 2f
                
                // Interpolate mid-point radius
                val midCookieRadius = baseRadius * (0.94f + 0.06f * cos(midAngle * lobes))
                val midSquircleRadius = baseRadius * (1f / Math.pow((Math.pow(Math.abs(cos(midAngle)).toDouble(), squirclePower) + Math.pow(Math.abs(sin(midAngle)).toDouble(), squirclePower)), 1.0/squirclePower)).toFloat() * 0.9f
                val midRadius = midSquircleRadius * (1f - progress) + midCookieRadius * progress
                
                val midX = centerX + midRadius * cos(midAngle)
                val midY = centerY + midRadius * sin(midAngle)
                
                path.quadraticTo(midX, midY, x, y)
            }
        }
        
        // Close the path smoothly
        val lastAngle = (points - 1) * angleStep
        val firstAngle = 2 * PI.toFloat()
        val midAngle = (lastAngle + firstAngle) / 2f
        
        val lobes = 12
        val squirclePower = 2.4
        val midCookieRadius = baseRadius * (0.94f + 0.06f * cos(midAngle * lobes))
        val midSquircleRadius = baseRadius * (1f / Math.pow((Math.pow(Math.abs(cos(midAngle)).toDouble(), squirclePower) + Math.pow(Math.abs(sin(midAngle)).toDouble(), squirclePower)), 1.0/squirclePower)).toFloat() * 0.9f
        val midRadius = midSquircleRadius * (1f - progress) + midCookieRadius * progress
        
        val midX = centerX + midRadius * cos(midAngle)
        val midY = centerY + midRadius * sin(midAngle)
        
        val startRadius = (baseRadius * 0.9f) * (1f - progress) + (baseRadius * (0.94f + 0.06f)) * progress
        path.quadraticTo(midX, midY, centerX + startRadius, centerY)
        
        path.close()
        return Outline.Generic(path)
    }
}
