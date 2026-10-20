package com.example.ui.theme

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
    primary = OrangePrimaryDark,
    onPrimary = OrangeOnPrimaryDark,
    primaryContainer = OrangePrimaryContainerDark,
    onPrimaryContainer = OrangeOnPrimaryContainerDark,
    secondary = OrangeSecondaryDark,
    onSecondary = OrangeOnSecondaryDark,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OrangeOnSecondaryContainerDark,
    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    background = OrangeBackgroundDark,
    onBackground = OrangeOnBackgroundDark,
    surface = OrangeSurfaceDark,
    onSurface = OrangeOnSurfaceDark,
    surfaceVariant = OrangeSurfaceVariantDark,
    onSurfaceVariant = OrangeOnSurfaceVariantDark,
    outline = OrangeOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = OrangeOnSecondary,
    secondaryContainer = OrangeSecondaryContainer,
    onSecondaryContainer = OrangeOnSecondaryContainer,
    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    background = OrangeBackgroundLight,
    onBackground = OrangeOnBackgroundLight,
    surface = OrangeSurfaceLight,
    onSurface = OrangeOnSurfaceLight,
    surfaceVariant = OrangeSurfaceVariantLight,
    onSurfaceVariant = OrangeOnSurfaceVariantLight,
    outline = OrangeOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    // Preserve custom orange theme identity requested by the user
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
