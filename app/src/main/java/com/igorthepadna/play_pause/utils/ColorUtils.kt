package com.igorthepadna.play_pause.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkColors(
    val primary: Color,
    val secondary: Color
)

@Composable
fun rememberArtworkColors(artworkUri: Uri?, defaultPrimary: Color, defaultSecondary: Color): ArtworkColors {
    val context = androidx.compose.ui.platform.LocalContext.current
    var colors by remember(artworkUri) { 
        mutableStateOf(ArtworkColors(defaultPrimary, defaultSecondary)) 
    }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null) {
            colors = ArtworkColors(defaultPrimary, defaultSecondary)
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .allowHardware(false) 
                .size(120, 120) 
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(32) 
                        .generate()
                    
                    val swatches = palette.swatches.sortedByDescending { it.population }
                    
                    // Primary extraction (background tint)
                    val primaryInt = palette.getDarkMutedSwatch()?.rgb
                        ?: palette.getMutedSwatch()?.rgb
                        ?: palette.getDominantSwatch()?.rgb
                        ?: defaultPrimary.toArgb()

                    // Accent extraction
                    val accentSwatch = palette.getVibrantSwatch()
                        ?: palette.getLightVibrantSwatch()
                        ?: palette.getDarkVibrantSwatch()
                        ?: swatches.firstOrNull { it.hsl[1] >= 0.15f }
                        ?: palette.getDominantSwatch()

                    val baseSecondary = accentSwatch?.rgb ?: defaultSecondary.toArgb()
                    
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(baseSecondary, hsl)

                    // No hard fallback to theme color for grayscale.
                    // Instead, we allow the extracted gray but normalize it for UI visibility.
                    // This keeps the B&W "feel" without shifting to random colors like Red.
                    
                    // 1. Force saturation to 0 if it's very low to kill "random red" shifts
                    if (hsl[1] < 0.12f) {
                        hsl[1] = 0f 
                    } else {
                        // 2. If it is colorful, ensure it's punchy enough
                        hsl[1] = hsl[1].coerceIn(0.45f, 0.9f)
                    }

                    // 3. Ensure it's not too dark or too bright for text/pills
                    hsl[2] = hsl[2].coerceIn(0.5f, 0.65f)
                    
                    val finalSecondary = Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))

                    colors = ArtworkColors(
                        primary = Color(primaryInt),
                        secondary = finalSecondary
                    )
                }
            }
        }
    }

    return colors
}
