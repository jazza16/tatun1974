package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic animated weather icon component that reflects the weather conditions
 * fetched from the BMKG / Open-Meteo API (e.g. Sunny, Partly Cloudy, Cloudy, Rain, Thunderstorm, Fog).
 */
@Composable
fun DynamicWeatherIcon(
    weatherCode: Int,
    isDay: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnimation")

    // Continuous rotation for sun rays
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotation"
    )

    // Pulsing glow for sun / moon
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Gentle cloud drifting
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudDrift"
    )

    // Rain drop falling progress (0f to 1f)
    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainProgress"
    )

    // Lightning flash alpha
    val lightningFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightningFlash"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            when (weatherCode) {
                // 0: Sunny / Clear
                0 -> {
                    if (isDay) {
                        drawDynamicSun(w, h, sunRotation, glowPulse)
                    } else {
                        drawDynamicMoon(w, h, glowPulse)
                    }
                }
                // 1, 2: Partly Cloudy / Cerah Berawan
                1, 2 -> {
                    drawDynamicPartlyCloudy(w, h, sunRotation, cloudDrift, isDay)
                }
                // 3: Overcast / Mendung
                3 -> {
                    drawDynamicOvercast(w, h, cloudDrift)
                }
                // 45, 48: Fog / Mist / Berkabut
                45, 48 -> {
                    drawDynamicFog(w, h, cloudDrift)
                }
                // 51, 53, 55: Drizzle / Gerimis
                51, 53, 55 -> {
                    drawDynamicRain(w, h, cloudDrift, rainProgress, isHeavy = false)
                }
                // 61, 63, 65, 80, 81, 82: Rain / Hujan
                61, 63, 65, 80, 81, 82 -> {
                    val isHeavy = weatherCode == 65 || weatherCode == 82
                    drawDynamicRain(w, h, cloudDrift, rainProgress, isHeavy = isHeavy)
                }
                // 95, 96, 99: Thunderstorm / Badai Petir
                95, 96, 99 -> {
                    drawDynamicThunderstorm(w, h, cloudDrift, rainProgress, lightningFlash)
                }
                // Default / fallback: Partly Cloudy
                else -> {
                    drawDynamicPartlyCloudy(w, h, sunRotation, cloudDrift, isDay)
                }
            }
        }
    }
}

// ------------------- DRAWING HELPERS -------------------

private fun DrawScope.drawDynamicSun(w: Float, h: Float, rotationDeg: Float, pulse: Float) {
    val center = Offset(w * 0.5f, h * 0.5f)
    val sunRadius = w * 0.22f * pulse

    // Glow Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.5f), Color.Transparent),
            center = center,
            radius = w * 0.48f
        ),
        radius = w * 0.48f,
        center = center
    )

    // Sun Rays
    val rayCount = 8
    val rayLength = w * 0.12f
    val rayStart = sunRadius + w * 0.04f

    for (i in 0 until rayCount) {
        val angle = Math.toRadians((i * (360.0 / rayCount) + rotationDeg))
        val startX = (center.x + rayStart * cos(angle)).toFloat()
        val startY = (center.y + rayStart * sin(angle)).toFloat()
        val endX = (center.x + (rayStart + rayLength) * cos(angle)).toFloat()
        val endY = (center.y + (rayStart + rayLength) * sin(angle)).toFloat()

        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = w * 0.055f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    // Core Sun Sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFBEB), Color(0xFFF59E0B), Color(0xFFD97706)),
            center = Offset(center.x - sunRadius * 0.25f, center.y - sunRadius * 0.25f),
            radius = sunRadius * 1.2f
        ),
        radius = sunRadius,
        center = center
    )
}

private fun DrawScope.drawDynamicMoon(w: Float, h: Float, pulse: Float) {
    val center = Offset(w * 0.48f, h * 0.48f)
    val r = w * 0.26f * pulse

    // Night soft glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF93C5FD).copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = w * 0.45f
        ),
        radius = w * 0.45f,
        center = center
    )

    // Full Moon base
    drawCircle(
        color = Color(0xFFE2E8F0),
        radius = r,
        center = center
    )

    // Shadow cutout to make crescent
    drawCircle(
        color = Color(0xFF1E293B),
        radius = r * 0.9f,
        center = Offset(center.x + r * 0.55f, center.y - r * 0.25f)
    )

    // Small twinkling stars
    drawCircle(color = Color(0xFFFDE047), radius = w * 0.025f, center = Offset(w * 0.78f, h * 0.25f))
    drawCircle(color = Color(0xFFFDE047), radius = w * 0.018f, center = Offset(w * 0.82f, h * 0.45f))
    drawCircle(color = Color(0xFFFDE047), radius = w * 0.02f, center = Offset(w * 0.25f, h * 0.72f))
}

private fun DrawScope.drawDynamicPartlyCloudy(
    w: Float,
    h: Float,
    sunRotation: Float,
    cloudDrift: Float,
    isDay: Boolean
) {
    // Sun positioned behind and above cloud
    val sunCenter = Offset(w * 0.65f, h * 0.35f)
    val sunRadius = w * 0.19f

    // Glowing sun rays
    if (isDay) {
        val rayCount = 6
        for (i in 0 until rayCount) {
            val angle = Math.toRadians((i * (360.0 / rayCount) + sunRotation))
            val startX = (sunCenter.x + (sunRadius + w * 0.02f) * cos(angle)).toFloat()
            val startY = (sunCenter.y + (sunRadius + w * 0.02f) * sin(angle)).toFloat()
            val endX = (sunCenter.x + (sunRadius + w * 0.09f) * cos(angle)).toFloat()
            val endY = (sunCenter.y + (sunRadius + w * 0.09f) * sin(angle)).toFloat()

            drawLine(
                color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = w * 0.045f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B)),
                center = sunCenter,
                radius = sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )
    } else {
        drawCircle(
            color = Color(0xFFCBD5E1),
            radius = sunRadius,
            center = sunCenter
        )
    }

    // Forefront fluffy drifting white cloud
    drawFluffyCloud(
        offsetX = w * 0.05f + cloudDrift,
        offsetY = h * 0.36f,
        width = w * 0.8f,
        height = h * 0.45f,
        topColor = Color(0xFFFFFFFF),
        bottomColor = Color(0xFFE2E8F0)
    )
}

private fun DrawScope.drawDynamicOvercast(w: Float, h: Float, cloudDrift: Float) {
    // Back darker cloud
    drawFluffyCloud(
        offsetX = w * 0.2f - cloudDrift * 0.6f,
        offsetY = h * 0.2f,
        width = w * 0.75f,
        height = h * 0.45f,
        topColor = Color(0xFF94A3B8),
        bottomColor = Color(0xFF64748B)
    )

    // Front cloud
    drawFluffyCloud(
        offsetX = w * 0.05f + cloudDrift,
        offsetY = h * 0.35f,
        width = w * 0.85f,
        height = h * 0.5f,
        topColor = Color(0xFFE2E8F0),
        bottomColor = Color(0xFF94A3B8)
    )
}

private fun DrawScope.drawDynamicFog(w: Float, h: Float, cloudDrift: Float) {
    // Upper cloud base
    drawFluffyCloud(
        offsetX = w * 0.1f,
        offsetY = h * 0.15f,
        width = w * 0.8f,
        height = h * 0.4f,
        topColor = Color(0xFFF1F5F9),
        bottomColor = Color(0xFFCBD5E1)
    )

    // Horizontal drifting fog streaks
    val streakY1 = h * 0.62f
    val streakY2 = h * 0.74f
    val streakY3 = h * 0.86f

    drawLine(
        color = Color(0xFF94A3B8),
        start = Offset(w * 0.15f + cloudDrift, streakY1),
        end = Offset(w * 0.85f + cloudDrift, streakY1),
        strokeWidth = w * 0.06f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )

    drawLine(
        color = Color(0xFF64748B).copy(alpha = 0.8f),
        start = Offset(w * 0.25f - cloudDrift, streakY2),
        end = Offset(w * 0.75f - cloudDrift, streakY2),
        strokeWidth = w * 0.055f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )

    drawLine(
        color = Color(0xFF94A3B8).copy(alpha = 0.7f),
        start = Offset(w * 0.2f + cloudDrift * 0.5f, streakY3),
        end = Offset(w * 0.8f + cloudDrift * 0.5f, streakY3),
        strokeWidth = w * 0.05f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}

private fun DrawScope.drawDynamicRain(
    w: Float,
    h: Float,
    cloudDrift: Float,
    rainProgress: Float,
    isHeavy: Boolean
) {
    // Stormy rain cloud
    drawFluffyCloud(
        offsetX = w * 0.06f + cloudDrift,
        offsetY = h * 0.18f,
        width = w * 0.88f,
        height = h * 0.48f,
        topColor = if (isHeavy) Color(0xFF64748B) else Color(0xFF94A3B8),
        bottomColor = if (isHeavy) Color(0xFF334155) else Color(0xFF475569)
    )

    // Falling raindrops
    val drops = if (isHeavy) {
        listOf(
            Triple(0.22f, 0.0f, 0.25f),
            Triple(0.38f, 0.35f, 0.28f),
            Triple(0.54f, 0.15f, 0.26f),
            Triple(0.70f, 0.50f, 0.27f),
            Triple(0.82f, 0.25f, 0.24f)
        )
    } else {
        listOf(
            Triple(0.28f, 0.0f, 0.22f),
            Triple(0.50f, 0.4f, 0.24f),
            Triple(0.72f, 0.2f, 0.23f)
        )
    }

    val rainStartY = h * 0.65f
    val rainTotalTravel = h * 0.32f

    for ((dropXFrac, offsetPhase, lengthFrac) in drops) {
        val currentProgress = (rainProgress + offsetPhase) % 1.0f
        val dropStartY = rainStartY + currentProgress * rainTotalTravel
        val dropLength = h * lengthFrac
        val dropX = w * dropXFrac - (currentProgress * w * 0.06f) // subtle slant

        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.2f), Color(0xFF0284C7)),
                startY = dropStartY,
                endY = dropStartY + dropLength
            ),
            start = Offset(dropX, dropStartY),
            end = Offset(dropX - w * 0.03f, dropStartY + dropLength),
            strokeWidth = if (isHeavy) w * 0.045f else w * 0.035f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun DrawScope.drawDynamicThunderstorm(
    w: Float,
    h: Float,
    cloudDrift: Float,
    rainProgress: Float,
    lightningFlash: Float
) {
    // Dark ominous storm cloud
    drawFluffyCloud(
        offsetX = w * 0.05f + cloudDrift,
        offsetY = h * 0.15f,
        width = w * 0.9f,
        height = h * 0.48f,
        topColor = Color(0xFF475569),
        bottomColor = Color(0xFF1E293B)
    )

    // Electric Lightning Bolt
    val isFlashing = (lightningFlash in 0.1f..0.35f) || (lightningFlash in 0.5f..0.7f)
    if (isFlashing) {
        val boltPath = Path().apply {
            moveTo(w * 0.52f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.70f)
            lineTo(w * 0.50f, h * 0.70f)
            lineTo(w * 0.38f, h * 0.95f)
            lineTo(w * 0.56f, h * 0.72f)
            lineTo(w * 0.47f, h * 0.72f)
            close()
        }

        drawPath(
            path = boltPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))
            ),
            style = Fill
        )

        // Glow outline
        drawPath(
            path = boltPath,
            color = Color(0xFFFFFFFF),
            style = Stroke(width = w * 0.025f)
        )
    }

    // Raindrops around lightning
    val drops = listOf(
        Pair(0.25f, 0.0f),
        Pair(0.75f, 0.3f),
        Pair(0.62f, 0.6f)
    )
    val rainStartY = h * 0.64f
    val rainTravel = h * 0.32f

    for ((dropXFrac, offsetPhase) in drops) {
        val currentProgress = (rainProgress + offsetPhase) % 1.0f
        val dropStartY = rainStartY + currentProgress * rainTravel
        val dropX = w * dropXFrac

        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(dropX, dropStartY),
            end = Offset(dropX - w * 0.025f, dropStartY + h * 0.16f),
            strokeWidth = w * 0.035f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFluffyCloud(
    offsetX: Float,
    offsetY: Float,
    width: Float,
    height: Float,
    topColor: Color,
    bottomColor: Color
) {
    val cloudBrush = Brush.verticalGradient(
        colors = listOf(topColor, bottomColor),
        startY = offsetY,
        endY = offsetY + height
    )

    val baseHeight = height * 0.45f
    val baseY = offsetY + height - baseHeight

    // Cloud rounded bottom pill
    drawRoundRect(
        brush = cloudBrush,
        topLeft = Offset(offsetX, baseY),
        size = Size(width, baseHeight),
        cornerRadius = CornerRadius(baseHeight * 0.5f, baseHeight * 0.5f)
    )

    // Left puff
    drawCircle(
        brush = cloudBrush,
        radius = height * 0.36f,
        center = Offset(offsetX + width * 0.28f, offsetY + height * 0.55f)
    )

    // Center tall puff
    drawCircle(
        brush = cloudBrush,
        radius = height * 0.46f,
        center = Offset(offsetX + width * 0.52f, offsetY + height * 0.45f)
    )

    // Right puff
    drawCircle(
        brush = cloudBrush,
        radius = height * 0.32f,
        center = Offset(offsetX + width * 0.74f, offsetY + height * 0.60f)
    )
}
