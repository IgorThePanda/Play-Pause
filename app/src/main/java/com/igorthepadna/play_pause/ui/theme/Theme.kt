package com.igorthepadna.play_pause.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.igorthepadna.play_pause.ColorSchemeType

// --- PURPLE (DEFAULT) ---
private val PurpleLight = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = SoftPink,
    onPrimaryContainer = Color(0xFF21005E),
    secondary = MediumPurple,
    onSecondary = Color.White,
    secondaryContainer = BrightPink,
    onSecondaryContainer = Color(0xFF2E1139),
    surface = Color(0xFFFFF7FF),
    background = Color(0xFFFFF7FF),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

private val PurpleDark = darkColorScheme(
    primary = MediumPurple,
    onPrimary = Color(0xFF31006E),
    primaryContainer = DeepPurple,
    onPrimaryContainer = SoftPink,
    secondary = BrightPink,
    onSecondary = Color(0xFF55004A),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = BrightPink,
    surface = Color(0xFF141218),
    background = Color(0xFF141218),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B)
)

// --- BLUE ---
private val BlueLight = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFF0F4FF),
    background = Color(0xFFF0F4FF),
    surfaceContainer = Color(0xFFE1E9F5),
    surfaceContainerHigh = Color(0xFFD6DFEB),
    surfaceContainerHighest = Color(0xFFCCD6E1)
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBCC8DB),
    onSecondary = Color(0xFF263141),
    surface = Color(0xFF0B141D),
    background = Color(0xFF0B141D),
    surfaceContainer = Color(0xFF1A232E),
    surfaceContainerHigh = Color(0xFF242D39),
    surfaceContainerHighest = Color(0xFF2E3744)
)

// --- GREEN ---
private val GreenLight = lightColorScheme(
    primary = Color(0xFF006D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF98F6AB),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF506352),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFF0FBF4),
    background = Color(0xFFF0FBF4),
    surfaceContainer = Color(0xFFE1F5EA),
    surfaceContainerHigh = Color(0xFFD6EBE1),
    surfaceContainerHighest = Color(0xFFCCE1D8)
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFF7DDA91),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005226),
    onPrimaryContainer = Color(0xFF98F6AB),
    secondary = Color(0xFFB7CCB8),
    onSecondary = Color(0xFF233426),
    surface = Color(0xFF0C150F),
    background = Color(0xFF0C150F),
    surfaceContainer = Color(0xFF1A2E23),
    surfaceContainerHigh = Color(0xFF24392D),
    surfaceContainerHighest = Color(0xFF2E4437)
)

// --- ORANGE ---
private val OrangeLight = lightColorScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC1),
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Color(0xFF715A41),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFF8F2),
    background = Color(0xFFFFF8F2),
    surfaceContainer = Color(0xFFF5E9E1),
    surfaceContainerHigh = Color(0xFFEBDFD6),
    surfaceContainerHighest = Color(0xFFE1D6CC)
)

private val OrangeDark = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF693C00),
    onPrimaryContainer = Color(0xFFFFDCC1),
    secondary = Color(0xFFDFC2A2),
    onSecondary = Color(0xFF3F2D17),
    surface = Color(0xFF17130B),
    background = Color(0xFF17130B),
    surfaceContainer = Color(0xFF2E231A),
    surfaceContainerHigh = Color(0xFF392D24),
    surfaceContainerHighest = Color(0xFF44372E)
)

@Composable
fun PlayPauseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorSchemeType: ColorSchemeType = ColorSchemeType.DYNAMIC,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && colorSchemeType == ColorSchemeType.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            when (colorSchemeType) {
                ColorSchemeType.PURPLE -> if (darkTheme) PurpleDark else PurpleLight
                ColorSchemeType.BLUE -> if (darkTheme) BlueDark else BlueLight
                ColorSchemeType.GREEN -> if (darkTheme) GreenDark else GreenLight
                ColorSchemeType.ORANGE -> if (darkTheme) OrangeDark else OrangeLight
                else -> if (darkTheme) PurpleDark else PurpleLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
