package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment

/**
 * Standard Android Material 3 Window Width Size Classes.
 * - Compact: Normal portrait phones (< 600dp)
 * - Medium: Large phones in landscape, foldables unfolded, smaller tablets (600dp ..< 840dp)
 * - Expanded: Standard 10-12" tablets, Desktop, ChromeOS, Samsung DeX (>= 840dp)
 */
enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded
}

/**
 * Standard Android Material 3 Window Height Size Classes.
 * - Compact: Phone in landscape mode (< 480dp)
 * - Medium: Normal portrait phone, smaller tablet (480dp ..< 900dp)
 * - Expanded: Large tablet in portrait, tall displays (>= 900dp)
 */
enum class WindowHeightSizeClass {
    Compact,
    Medium,
    Expanded
}

data class WindowAdaptiveInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val maxWidth: Dp,
    val maxHeight: Dp
) {
    val isCompact: Boolean get() = widthSizeClass == WindowWidthSizeClass.Compact
    val isMedium: Boolean get() = widthSizeClass == WindowWidthSizeClass.Medium
    val isExpanded: Boolean get() = widthSizeClass == WindowWidthSizeClass.Expanded

    val isTabletOrFoldable: Boolean get() = maxWidth >= 600.dp
    val isExpandedTablet: Boolean get() = maxWidth >= 840.dp
    val isLandscapePhone: Boolean get() = maxHeight < 500.dp && maxWidth > maxHeight

    /**
     * Recommended maximum reading and content container width to prevent awkward edge-to-edge stretching
     * on large screens while retaining full responsive fluid behavior.
     */
    val optimalContentWidth: Dp
        get() = when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> Dp.Unspecified
            WindowWidthSizeClass.Medium -> 720.dp
            WindowWidthSizeClass.Expanded -> 880.dp
        }

    /**
     * Optimal number of columns for grid layouts (such as 15 Kecamatan or emergency contacts).
     */
    val optimalColumns: Int
        get() = when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> 1
            WindowWidthSizeClass.Medium -> 2
            WindowWidthSizeClass.Expanded -> 3
        }
}

/**
 * Derives and remembers [WindowAdaptiveInfo] based on the current container constraints.
 */
@Composable
fun rememberWindowAdaptiveInfo(maxWidth: Dp, maxHeight: Dp): WindowAdaptiveInfo {
    return remember(maxWidth, maxHeight) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthSizeClass.Compact
            maxWidth < 840.dp -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
        val heightClass = when {
            maxHeight < 480.dp -> WindowHeightSizeClass.Compact
            maxHeight < 900.dp -> WindowHeightSizeClass.Medium
            else -> WindowHeightSizeClass.Expanded
        }
        WindowAdaptiveInfo(
            widthSizeClass = widthClass,
            heightSizeClass = heightClass,
            maxWidth = maxWidth,
            maxHeight = maxHeight
        )
    }
}

/**
 * Helper extension to constrain content within a professional maximum width on large screens (tablets/foldables)
 * while ensuring fluid 100% full width on compact phones.
 */
fun Modifier.adaptiveContainer(adaptiveInfo: WindowAdaptiveInfo, maxWidth: Dp = 860.dp): Modifier {
    return if (adaptiveInfo.isTabletOrFoldable) {
        this
            .fillMaxWidth()
            .widthIn(max = maxWidth)
    } else {
        this.fillMaxWidth()
    }
}
