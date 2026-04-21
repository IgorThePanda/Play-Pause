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
    val secondary: Color,
    val tertiary: Color
)

@Composable
fun rememberArtworkColors(
    artworkUri: Uri?,
    defaultPrimary: Color,
    defaultSecondary: Color,
    defaultTertiary: Color = defaultSecondary
): ArtworkColors {
    val context = androidx.compose.ui.platform.LocalContext.current
    var colors by remember(artworkUri) {
        mutableStateOf(ArtworkColors(defaultPrimary, defaultSecondary, defaultTertiary)) 
    }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null) {
            colors = ArtworkColors(defaultPrimary, defaultSecondary, defaultTertiary)
            return@LaunchedEffect
        }

        // Immediately reset to defaults when URI changes to avoid color "leakage" from previous artwork
        colors = ArtworkColors(defaultPrimary, defaultSecondary, defaultTertiary)

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

                    // Accent extraction (Secondary)
                    val accentSwatch = palette.getVibrantSwatch()
                        ?: palette.getLightVibrantSwatch()
                        ?: palette.getDarkVibrantSwatch()
                        ?: swatches.firstOrNull { it.hsl[1] >= 0.15f }
                        ?: palette.getDominantSwatch()

                    val baseSecondary = accentSwatch?.rgb ?: defaultSecondary.toArgb()
                    
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(baseSecondary, hsl)

                    if (hsl[1] < 0.12f) {
                        hsl[1] = 0f
                    } else {
                        hsl[1] = hsl[1].coerceIn(0.35f, 0.95f)
                    }
                    hsl[2] = hsl[2].coerceIn(0.4f, 0.75f)
                    val finalSecondary = Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))

                    // Tertiary extraction (Different from secondary)
                    val tertiarySwatch = palette.getLightMutedSwatch()
                        ?: palette.getMutedSwatch()
                        ?: swatches.find { it.rgb != accentSwatch?.rgb }
                        ?: palette.getDominantSwatch()

                    val baseTertiary = tertiarySwatch?.rgb ?: defaultSecondary.toArgb()
                    val hslT = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(baseTertiary, hslT)
                    if (hslT[1] < 0.12f) {
                        hslT[1] = 0f
                    } else {
                        hslT[1] = hslT[1].coerceIn(0.35f, 0.95f)
                    }
                    hslT[2] = hslT[2].coerceIn(0.4f, 0.75f)
                    val finalTertiary = Color(androidx.core.graphics.ColorUtils.HSLToColor(hslT))

                    colors = ArtworkColors(
                        primary = Color(primaryInt),
                        secondary = finalSecondary,
                        tertiary = finalTertiary
                    )
                }
            }
        }
    }

    return colors
}
