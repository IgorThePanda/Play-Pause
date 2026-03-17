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
                .allowHardware(false) // Required to extract pixels
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    
                    // Increased contrast color extraction:
                    // Primary: Prefer a Dark Muted color (deep, sophisticated base)
                    var primaryInt = palette.getDarkMutedColor(
                        palette.getMutedColor(
                            palette.getDominantColor(defaultPrimary.toArgb())
                        )
                    )
                    
                    // Secondary: Prefer a Light Vibrant color (bright, popping accent)
                    var secondaryInt = palette.getLightVibrantColor(
                        palette.getVibrantColor(
                            palette.getDominantColor(defaultSecondary.toArgb())
                        )
                    )

                    // Greyscale check: If colors have very low saturation, they are "unusable" for B&W covers.
                    // We check the saturation of the extracted secondary color.
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(secondaryInt, hsv)
                    val saturation = hsv[1]

                    if (saturation < 0.15f) {
                        // Fallback to default theme colors if the art is essentially Black & White
                        colors = ArtworkColors(defaultPrimary, defaultSecondary)
                    } else {
                        colors = ArtworkColors(Color(primaryInt), Color(secondaryInt))
                    }
                }
            }
        }
    }

    return colors
}
