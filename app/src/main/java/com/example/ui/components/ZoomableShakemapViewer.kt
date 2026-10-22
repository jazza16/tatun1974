package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.model.BmkgEarthquake
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherSunGold
import java.util.Locale

/**
 * Interactive Zoomable BMKG Shake Map Viewer that allows users to pinch-to-zoom,
 * double-tap to zoom, pan, and use dedicated Zoom In (+) / Zoom Out (-) / Reset buttons.
 */
@Composable
fun ZoomableShakemapViewer(
    earthquake: BmkgEarthquake,
    modifier: Modifier = Modifier,
    height: Dp = 320.dp
) {
    var isFullscreenOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .testTag("zoomable_shakemap_container")
    ) {
        ShakemapContent(
            earthquake = earthquake,
            modifier = Modifier.fillMaxSize(),
            onToggleFullscreen = { isFullscreenOpen = true }
        )
    }

    // Fullscreen Dialog Modal for maximum zoomability
    if (isFullscreenOpen) {
        Dialog(
            onDismissRequest = { isFullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ShakemapContent(
                        earthquake = earthquake,
                        modifier = Modifier.fillMaxSize(),
                        isFullscreen = true,
                        onToggleFullscreen = { isFullscreenOpen = false }
                    )

                    // Close Fullscreen Button
                    IconButton(
                        onClick = { isFullscreenOpen = false },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Peta",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShakemapContent(
    earthquake: BmkgEarthquake,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "zoomScaleAnim"
    )

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(Color(0xFF0F172A)) // Dark tactical background
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // Double tap & drag gesture handler
        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.4f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1.0f, 5.0f)
                    scale = newScale

                    val maxPanX = (maxWidthPx * (newScale - 1f)) / 2f
                    val maxPanY = (maxHeightPx * (newScale - 1f)) / 2f

                    offsetX = (offsetX + pan.x * newScale).coerceIn(-maxPanX, maxPanX)
                    offsetY = (offsetY + pan.y * newScale).coerceIn(-maxPanY, maxPanY)
                }
            }

        // The Map Image / Fallback Graphic with transform matrix
        Box(
            modifier = gestureModifier.graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = offsetX
                translationY = offsetY
            },
            contentAlignment = Alignment.Center
        ) {
            if (!earthquake.shakemapUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = earthquake.shakemapUrl,
                    contentDescription = "Peta Guncangan BMKG (Shakemap)",
                    contentScale = if (isFullscreen) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = OrangePrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Mengunduh Peta Guncangan BMKG...",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    error = {
                        InteractiveSyntheticShakemap(earthquake = earthquake)
                    }
                )
            } else {
                InteractiveSyntheticShakemap(earthquake = earthquake)
            }
        }

        // Top-Left Floating Info Chips (Epicenter & Magnitude)
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFDC2626), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BMKG SHAKEMAP TERKINI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "M ${String.format(Locale.US, "%.1f", earthquake.magnitude)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF87171)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kedalaman ${earthquake.depth}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = "${earthquake.distanceKmToMukomuko} km dari Mukomuko",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (earthquake.isNearMukomuko) Color(0xFFFCA5A5) else Color(0xFF93C5FD)
                )
            }
        }

        // Top-Right Interactive Zoom Control Buttons (+, -, Reset, Fullscreen)
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In (+)
                IconButton(
                    onClick = {
                        scale = (scale + 0.6f).coerceAtMost(5.0f)
                    },
                    modifier = Modifier.size(34.dp).testTag("btn_zoom_in_shakemap")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Perbesar Peta",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom Out (-)
                IconButton(
                    onClick = {
                        scale = (scale - 0.6f).coerceAtLeast(1.0f)
                        if (scale == 1.0f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    modifier = Modifier.size(34.dp).testTag("btn_zoom_out_shakemap")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Perkecil Peta",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset (1.0x)
                if (scale > 1.05f) {
                    IconButton(
                        onClick = {
                            scale = 1.0f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.size(34.dp).testTag("btn_reset_zoom_shakemap")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Zoom",
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Fullscreen toggle
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(34.dp).testTag("btn_fullscreen_shakemap")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Keluar Layar Penuh" else "Layar Penuh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Instruction Bar: Gestures Hint & Current Zoom Indicator
        Surface(
            color = Color.Black.copy(alpha = 0.68f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .padding(bottom = 10.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (scale > 1.05f) {
                        "Zoom: ${String.format(Locale.US, "%.1fx", scale)} • Geser untuk navigasi"
                    } else {
                        "Cubit (Pinch) atau Ketuk 2x untuk Zoom In Peta"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Fallback high-fidelity synthetic Shakemap graphic depicting
 * the Bengkulu / Mukomuko coastline, Indo-Australian tectonic subduction trench,
 * and concentric seismic MMI wave contours.
 */
@Composable
private fun InteractiveSyntheticShakemap(
    earthquake: BmkgEarthquake,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Ocean gradient background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0C4A6E))
                )
            )

            // Coastline contour (Sumatra West Coast)
            val coastPath = Path().apply {
                moveTo(w * 0.15f, 0f)
                cubicTo(w * 0.35f, h * 0.25f, w * 0.55f, h * 0.60f, w * 0.75f, h)
                lineTo(w, h)
                lineTo(w, 0f)
                close()
            }

            drawPath(
                path = coastPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF14532D).copy(alpha = 0.6f), Color(0xFF15803D).copy(alpha = 0.75f))
                )
            )

            // Tectonic Subduction Trench (Megathrust Line)
            val trenchPath = Path().apply {
                moveTo(w * 0.05f, 0f)
                cubicTo(w * 0.22f, h * 0.30f, w * 0.40f, h * 0.65f, w * 0.58f, h)
            }
            drawPath(
                path = trenchPath,
                color = Color(0xFFEF4444).copy(alpha = 0.7f),
                style = Stroke(width = 3.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )

            // Epicenter position calculation
            val epicX = w * 0.38f
            val epicY = h * 0.52f
            val epicCenter = Offset(epicX, epicY)

            // Concentric Seismic Isoseismal MMI Circles (Shakemap rings)
            val ringRadii = listOf(w * 0.12f, w * 0.22f, w * 0.34f, w * 0.48f)
            val ringColors = listOf(
                Color(0xFFDC2626).copy(alpha = 0.45f), // VI - VII MMI
                Color(0xFFEA580C).copy(alpha = 0.35f), // V MMI
                Color(0xFFF59E0B).copy(alpha = 0.25f), // IV MMI
                Color(0xFF38BDF8).copy(alpha = 0.18f)  // II - III MMI
            )

            ringRadii.forEachIndexed { idx, r ->
                drawCircle(
                    color = ringColors[idx],
                    radius = r,
                    center = epicCenter,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Mukomuko Location Dot & Label
            val mukomukoPt = Offset(w * 0.58f, h * 0.38f)
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 6.dp.toPx(),
                center = mukomukoPt
            )
            drawCircle(
                color = Color.Black,
                radius = 2.5.dp.toPx(),
                center = mukomukoPt
            )

            // Epicenter Star / Red Target
            drawCircle(
                color = Color(0xFFDC2626),
                radius = 9.dp.toPx(),
                center = epicCenter
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = epicCenter
            )
        }

        // Overlay Labels for Cities
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Daratan Mukomuko, Bengkulu",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
