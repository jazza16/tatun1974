package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BmkgEarthquake
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class WrsBasemapType(val displayName: String) {
    STREETS("Streets"),
    SATELLITE("Satelit"),
    GRAYSCALE("Grayscale")
}

/**
 * Interactive Geospatial GIS Map Viewer modeled directly after BMKG WRS-NTWC
 * (Warning Receiver System - National Tsunami Warning Center: inatews.bmkg.go.id/wrs).
 * Features multiple basemaps, subduction megathrust fault line, animated P/S wave fronts,
 * distance measurement to Mukomuko station, and pinch-to-zoom / pan navigation.
 */
@Composable
fun WrsNtwcMapView(
    earthquake: BmkgEarthquake,
    modifier: Modifier = Modifier,
    height: Dp = 330.dp
) {
    var isFullscreen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .testTag("wrs_ntwc_map_container")
    ) {
        WrsMapInnerContent(
            earthquake = earthquake,
            isFullscreen = false,
            onToggleFullscreen = { isFullscreen = true },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("wrs_ntwc_map_fullscreen")
            ) {
                WrsMapInnerContent(
                    earthquake = earthquake,
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun WrsMapInnerContent(
    earthquake: BmkgEarthquake,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var basemapType by remember { mutableStateOf(WrsBasemapType.STREETS) }
    var showMegathrust by remember { mutableStateOf(true) }
    var showWavefronts by remember { mutableStateOf(true) }

    // Map Transform state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Animated concentric wave transitions for P & S seismic waves
    val waveTransition = rememberInfiniteTransition(label = "seismic_wavefront")
    val pWaveProgress by waveTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p_wave"
    )
    val sWaveProgress by waveTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "s_wave"
    )
    val beaconPulse by waveTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.85f, 4.5f)
                        val maxPan = 400f * scale
                        offsetX = (offsetX + pan.x).coerceIn(-maxPan, maxPan)
                        offsetY = (offsetY + pan.y).coerceIn(-maxPan, maxPan)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.4f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.2f
                            }
                        }
                    )
                }
        ) {
            // Geospatial Canvas Layer
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Base Colors (Ocean & Land)
                drawBasemapBackground(basemapType, w, h)

                // Draw Coastlines and Island Polygons of Western Sumatra & Indonesia
                drawSumatraCoastlineAndIslands(basemapType, w, h)

                // Draw Sunda Megathrust / Mentawai Subduction Fault Zone
                if (showMegathrust) {
                    drawMegathrustFaultLine(w, h, basemapType)
                }

                // Reference coordinates: Mukomuko (-2.5833, 101.1167) and Epicenter
                val mukoCenter = Offset(w * 0.52f, h * 0.50f)

                // Calculate relative pixel position of Epicenter from Mukomuko
                val deltaLon = (earthquake.longitude - 101.1167).toFloat()
                val deltaLat = (earthquake.latitude - (-2.5833)).toFloat()

                // Scale factor for geographic coords to canvas
                val coordScale = w * 0.38f
                val epiCenter = Offset(
                    (mukoCenter.x + deltaLon * coordScale).coerceIn(w * 0.12f, w * 0.88f),
                    (mukoCenter.y - deltaLat * coordScale).coerceIn(h * 0.12f, h * 0.88f)
                )

                // Measurement line between Mukomuko & Epicenter
                drawMeasurementLine(mukoCenter, epiCenter, earthquake.distanceKmToMukomuko)

                // Animated Concentric Seismic Wavefronts (P & S waves)
                if (showWavefronts) {
                    drawSeismicWavefronts(epiCenter, pWaveProgress, sWaveProgress, earthquake.magnitude.toFloat())
                }

                // Mukomuko Station Marker
                drawMukomukoStationMarker(mukoCenter)

                // Earthquake Epicenter Beacon
                drawEpicenterBeacon(epiCenter, earthquake, beaconPulse)
            }

            // Top Bar Overlay: Basemap selector & Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Basemap Type Switcher Chips
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WrsBasemapType.values().forEach { type ->
                            val isSelected = basemapType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) OrangePrimary else Color.Transparent)
                                    .pointerInput(Unit) {
                                        detectTapGestures { basemapType = type }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = type.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Fullscreen toggle button
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("btn_toggle_map_fullscreen")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Layar Penuh Peta",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Floating Map Controls (Right Side: Zoom In, Zoom Out, Reset)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom In
                IconButton(
                    onClick = { scale = (scale + 0.4f).coerceAtMost(4.5f) },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("btn_map_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom Out
                IconButton(
                    onClick = { scale = (scale - 0.4f).coerceAtLeast(0.85f) },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("btn_map_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset Map
                IconButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("btn_map_reset_view")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Peta",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom Legend & Layer Info Overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Depth Legend
                        LegendDot(Color(0xFFEF4444), "< 60 km (Dangkal)")
                        LegendDot(Color(0xFFF59E0B), "60-300 km")
                        LegendDot(Color(0xFF10B981), "> 300 km")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFDC2626), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zona Megathrust Sunda & Sesar Mentawai",
                            fontSize = 9.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f))
    }
}

// ---------------- CANVAS DRAWING HELPERS ----------------

private fun DrawScope.drawBasemapBackground(type: WrsBasemapType, w: Float, h: Float) {
    val oceanColor = when (type) {
        WrsBasemapType.STREETS -> Color(0xFFCFE5F0)
        WrsBasemapType.SATELLITE -> Color(0xFF0C243B)
        WrsBasemapType.GRAYSCALE -> Color(0xFF18181B)
    }
    drawRect(color = oceanColor)

    // Latitude & Longitude Geodetic Grid
    val gridColor = when (type) {
        WrsBasemapType.STREETS -> Color(0xFFB4D2E3).copy(alpha = 0.4f)
        WrsBasemapType.SATELLITE -> Color(0xFF1E3A5F).copy(alpha = 0.5f)
        WrsBasemapType.GRAYSCALE -> Color(0xFF27272A).copy(alpha = 0.8f)
    }
    val gridStep = w / 6f
    for (i in 1..5) {
        val x = i * gridStep
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
    val gridStepY = h / 5f
    for (i in 1..4) {
        val y = i * gridStepY
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
}

private fun DrawScope.drawSumatraCoastlineAndIslands(type: WrsBasemapType, w: Float, h: Float) {
    val landColor = when (type) {
        WrsBasemapType.STREETS -> Color(0xFFE8E2D3)
        WrsBasemapType.SATELLITE -> Color(0xFF234B32)
        WrsBasemapType.GRAYSCALE -> Color(0xFF2A2A2E)
    }
    val coastBorderColor = when (type) {
        WrsBasemapType.STREETS -> Color(0xFF8FA89B)
        WrsBasemapType.SATELLITE -> Color(0xFF386B4B)
        WrsBasemapType.GRAYSCALE -> Color(0xFF4B4B52)
    }

    // Mainland West Sumatra & Bengkulu Polygon
    val mainlandPath = Path().apply {
        moveTo(w * 0.35f, 0f)
        lineTo(w * 0.44f, h * 0.22f)
        lineTo(w * 0.52f, h * 0.45f) // Mukomuko coastal strip
        lineTo(w * 0.60f, h * 0.65f) // Bengkulu central
        lineTo(w * 0.68f, h * 0.85f) // Bengkulu selatan / Manna
        lineTo(w * 0.78f, h)         // Towards Lampung
        lineTo(w, h)
        lineTo(w, 0f)
        close()
    }
    drawPath(mainlandPath, color = landColor)
    drawPath(mainlandPath, color = coastBorderColor, style = Stroke(width = 2.5f))

    // Mentawai Outer Arc Islands (Siberut, Sipora, Pagai) offshore
    val siberutPath = Path().apply {
        moveTo(w * 0.22f, h * 0.25f)
        lineTo(w * 0.28f, h * 0.35f)
        lineTo(w * 0.25f, h * 0.45f)
        lineTo(w * 0.18f, h * 0.35f)
        close()
    }
    drawPath(siberutPath, color = landColor)
    drawPath(siberutPath, color = coastBorderColor, style = Stroke(width = 1.5f))

    val siporaPagaiPath = Path().apply {
        moveTo(w * 0.28f, h * 0.50f)
        lineTo(w * 0.34f, h * 0.62f)
        lineTo(w * 0.31f, h * 0.72f)
        lineTo(w * 0.25f, h * 0.60f)
        close()
    }
    drawPath(siporaPagaiPath, color = landColor)
    drawPath(siporaPagaiPath, color = coastBorderColor, style = Stroke(width = 1.5f))

    // Pulau Enggano offshore southern Bengkulu
    val engganoPath = Path().apply {
        moveTo(w * 0.50f, h * 0.88f)
        lineTo(w * 0.54f, h * 0.90f)
        lineTo(w * 0.52f, h * 0.94f)
        lineTo(w * 0.48f, h * 0.92f)
        close()
    }
    drawPath(engganoPath, color = landColor)
    drawPath(engganoPath, color = coastBorderColor, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawMegathrustFaultLine(w: Float, h: Float, type: WrsBasemapType) {
    val faultColor = Color(0xFFEF4444)
    val trenchPath = Path().apply {
        moveTo(w * 0.10f, 0f)
        lineTo(w * 0.16f, h * 0.28f)
        lineTo(w * 0.22f, h * 0.55f)
        lineTo(w * 0.34f, h * 0.82f)
        lineTo(w * 0.45f, h)
    }
    drawPath(
        path = trenchPath,
        color = faultColor.copy(alpha = 0.8f),
        style = Stroke(
            width = 3.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
        )
    )
}

private fun DrawScope.drawMeasurementLine(from: Offset, to: Offset, distanceKm: Int) {
    drawLine(
        color = Color(0xFFF97316),
        start = from,
        end = to,
        strokeWidth = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )

    // Midpoint distance tag
    val midX = (from.x + to.x) / 2f
    val midY = (from.y + to.y) / 2f

    drawCircle(
        color = Color(0xFFEA580C),
        radius = 5f,
        center = Offset(midX, midY)
    )
}

private fun DrawScope.drawSeismicWavefronts(
    epicenter: Offset,
    pWaveProgress: Float,
    sWaveProgress: Float,
    magnitude: Float
) {
    val maxRadius = 140f + (magnitude * 18f)

    // Primary Wave (P-wave - faster, cyan/blue)
    val pRadius = maxRadius * pWaveProgress
    val pAlpha = (1f - pWaveProgress).coerceIn(0f, 0.85f)
    drawCircle(
        color = Color(0xFF06B6D4).copy(alpha = pAlpha),
        radius = pRadius,
        center = epicenter,
        style = Stroke(width = 2.5f)
    )

    // Secondary Wave (S-wave - slower, stronger, red/orange)
    val sRadius = maxRadius * sWaveProgress * 0.72f
    val sAlpha = (1f - sWaveProgress).coerceIn(0f, 0.95f)
    drawCircle(
        color = Color(0xFFEF4444).copy(alpha = sAlpha),
        radius = sRadius,
        center = epicenter,
        style = Stroke(width = 3.5f)
    )
}

private fun DrawScope.drawMukomukoStationMarker(center: Offset) {
    // Station halo
    drawCircle(
        color = Color(0xFF2563EB).copy(alpha = 0.25f),
        radius = 16f,
        center = center
    )
    drawCircle(
        color = Color(0xFF1D4ED8),
        radius = 8f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = center
    )
}

private fun DrawScope.drawEpicenterBeacon(
    center: Offset,
    earthquake: BmkgEarthquake,
    pulse: Float
) {
    val depthVal = earthquake.depth.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 15
    val beaconColor = when {
        depthVal < 60 -> Color(0xFFDC2626) // Dangkal - Red
        depthVal <= 300 -> Color(0xFFF59E0B) // Menengah - Orange
        else -> Color(0xFF10B981) // Dalam - Green
    }

    // Outer pulsing ring
    drawCircle(
        color = beaconColor.copy(alpha = pulse * 0.4f),
        radius = 28f * pulse,
        center = center
    )

    // Center focal point
    drawCircle(
        color = beaconColor,
        radius = 12f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 5f,
        center = center
    )
}
