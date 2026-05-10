package com.igorthepadna.play_pause.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.igorthepadna.play_pause.data.GridSizeMode

@Composable
fun calculateGridColumns(mode: GridSizeMode): Int {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    
    return when (mode) {
        GridSizeMode.SMALL -> (screenWidthDp / 100.dp).toInt().coerceAtLeast(3)
        GridSizeMode.MEDIUM -> (screenWidthDp / 150.dp).toInt().coerceAtLeast(2)
        GridSizeMode.LARGE -> (screenWidthDp / 200.dp).toInt().coerceAtLeast(1)
        GridSizeMode.AUTO -> {
            if (screenWidthDp < 600.dp) 2 else (screenWidthDp / 180.dp).toInt().coerceAtLeast(2)
        }
    }
}
