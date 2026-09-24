package com.example.proyectofinal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EcoDarkPrimary,
    secondary = EcoDarkSecondary,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = EcoGreenPrimary,
    secondary = EcoGreenSecondary,
    tertiary = EcoGreenTertiary
)

private val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastYellow,
    secondary = HighContrastWhite,
    background = HighContrastBlack,
    surface = HighContrastBlack,
    onPrimary = HighContrastBlack,
    onSecondary = HighContrastBlack,
    onBackground = HighContrastWhite,
    onSurface = HighContrastWhite
)

private val DeuteranopiaColorScheme = lightColorScheme(
    primary = DeuteranopiaPrimary,
    secondary = DeuteranopiaSecondary
)

private val ProtanopiaColorScheme = lightColorScheme(
    primary = ProtanopiaPrimary,
    secondary = ProtanopiaSecondary
)

@Composable
fun ProyectoFinalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    tipoDaltonismo: String = "None",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> HighContrastColorScheme
        tipoDaltonismo == "Deuteranopia" -> DeuteranopiaColorScheme
        tipoDaltonismo == "Protanopia" -> ProtanopiaColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}