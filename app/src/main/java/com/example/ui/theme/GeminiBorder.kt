package com.example.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GeminiBlue = Color(0xFF4285F4)
val GeminiPurple = Color(0xFF9B72CB)
val GeminiPink = Color(0xFFD96570)

/**
 * Modifier kustom untuk memberikan garis tepi (border) gradasi khas Gemini.
 * Terlihat kontras di Light Mode maupun Dark Mode tanpa menutupi konten di dalam komponen.
 */
fun Modifier.geminiGradientBorder(
    borderWidth: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = this.border(
    width = borderWidth,
    brush = Brush.linearGradient(
        colors = listOf(
            GeminiBlue,
            GeminiPurple,
            GeminiPink
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    ),
    shape = shape
)
