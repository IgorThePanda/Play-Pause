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
        GridSizeMode.SMALL -> (screenWidthDp / 80.dp).toInt().coerceAtLeast(5)
        GridSizeMode.MEDIUM -> (screenWidthDp / 120.dp).toInt().coerceAtLeast(3)
        GridSizeMode.LARGE -> (screenWidthDp / 240.dp).toInt().coerceAtLeast(1)
        GridSizeMode.AUTO -> {
            (screenWidthDp / 160.dp).toInt().coerceAtLeast(2)
        }
    }
}
