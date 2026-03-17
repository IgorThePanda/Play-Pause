package com.igorthepadna.play_pause.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(duration: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
