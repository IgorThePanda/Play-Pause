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
import coil.size.Size
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
                .size(100, 100) // Optimization: Decode a small version for palette extraction
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(12) // Optimization: Fewer colors to process
                        .generate()
                    
                    val primaryInt = palette.getDarkMutedColor(
                        palette.getMutedColor(
                            palette.getDominantColor(defaultPrimary.toArgb())
                        )
                    )
                    
                    val secondaryInt = palette.getLightVibrantColor(
                        palette.getVibrantColor(
                            palette.getDominantColor(defaultSecondary.toArgb())
                        )
                    )

                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(secondaryInt, hsv)
                    val saturation = hsv[1]

                    colors = if (saturation < 0.15f) {
                        ArtworkColors(defaultPrimary, defaultSecondary)
                    } else {
                        ArtworkColors(Color(primaryInt), Color(secondaryInt))
                    }
                }
            }
        }
    }

    return colors
}
