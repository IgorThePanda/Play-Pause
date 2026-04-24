package com.igorthepadna.play_pause.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.LruCache
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

private val colorCache = LruCache<Uri, ArtworkColors>(500)

@Composable
fun rememberArtworkColors(
    artworkUri: Uri?,
    defaultPrimary: Color,
    defaultSecondary: Color,
    defaultTertiary: Color = defaultSecondary
): ArtworkColors {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val initialColors = remember(artworkUri) {
        artworkUri?.let { colorCache.get(it) } ?: ArtworkColors(defaultPrimary, defaultSecondary, defaultTertiary)
    }
    
    var colors by remember(artworkUri) { mutableStateOf(initialColors) }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null) {
            colors = ArtworkColors(defaultPrimary, defaultSecondary, defaultTertiary)
            return@LaunchedEffect
        }

        val cached = colorCache.get(artworkUri)
        if (cached != null) {
            colors = cached
            return@LaunchedEffect
        }

        // Debounce extraction for new items to prevent stutter during rapid scrolling
        kotlinx.coroutines.delay(300)

        withContext(Dispatchers.Default) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .allowHardware(false) 
                .size(64, 64) // Even smaller for faster processing
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(12) // Further reduced for speed
                        .generate()
                    
                    val swatches = palette.swatches.sortedByDescending { it.population }
                    // ... extraction logic remains same for consistency ...

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

                    val resultColors = ArtworkColors(
                        primary = Color(primaryInt),
                        secondary = finalSecondary,
                        tertiary = finalTertiary
                    )
                    
                    colorCache.put(artworkUri, resultColors)
                    colors = resultColors
                }
            }
        }
    }

    return colors
}
