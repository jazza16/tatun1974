package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.util.CompassSensorManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalCompassBottomSheet(
    onDismiss: () -> Unit,
    onOpenEvacuationMap: () -> Unit = {},
    onCall117: () -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sensorManager = remember { CompassSensorManager(context) }

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    val state by sensorManager.compassState.collectAsState()
    var manualAngleOffset by remember { mutableFloatStateOf(0f) }

    // Smooth animation for needle
    val animatedAzimuth by animateFloatAsState(
        targetValue = state.azimuthDegrees + manualAngleOffset,
        animationSpec = tween(durationMillis = 150),
        label = "compassAzimuth"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("digital_compass_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Kompas Digital",
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Kompas Evakuasi Digital",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Navigasi Arah Aman Pesisir Mukomuko",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_compass_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Azimuth & Direction Badge
            Surface(
                color = OrangePrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${animatedAzimuth.toInt() % 360}° ${state.cardinalDirection.uppercase()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary
                        )
                        Text(
                            text = if (state.hasHardwareSensor) "Sensor Orientasi Hardware Aktif" else "Mode Sentuh / Interaktif",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        color = if (state.hasHardwareSensor) Color(0xFF16A34A) else OrangePrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.hasHardwareSensor) "AKURAT" else "ROTASI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Compass Dial Canvas
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(2.dp, OrangePrimary.copy(alpha = 0.4f), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            manualAngleOffset = (manualAngleOffset + dragAmount.x * 0.4f) % 360f
                            sensorManager.setManualHeading(animatedAzimuth)
                        }
                    }
                    .testTag("compass_dial_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2 - 8.dp.toPx()

                    // Outer Bezel Circle
                    drawCircle(
                        color = Color(0xFFEA580C).copy(alpha = 0.15f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw Ticks and Cardinal markers rotated by -animatedAzimuth
                    val currentRotationRad = Math.toRadians(-animatedAzimuth.toDouble())

                    for (degree in 0 until 360 step 15) {
                        val angleRad = Math.toRadians(degree.toDouble()) + currentRotationRad
                        val isMajor = degree % 90 == 0
                        val isSemiMajor = degree % 45 == 0
                        val tickLength = if (isMajor) 14.dp.toPx() else if (isSemiMajor) 9.dp.toPx() else 5.dp.toPx()
                        val tickColor = if (isMajor) Color(0xFFEA580C) else Color.Gray.copy(alpha = 0.5f)
                        val strokeWidth = if (isMajor) 2.5f else 1.2f

                        val startX = center.x + (radius - tickLength) * sin(angleRad).toFloat()
                        val startY = center.y - (radius - tickLength) * cos(angleRad).toFloat()
                        val endX = center.x + radius * sin(angleRad).toFloat()
                        val endY = center.y - radius * cos(angleRad).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth
                        )

                        // Draw Cardinal text (U, T, S, B)
                        if (isMajor) {
                            val textRadius = radius - 24.dp.toPx()
                            val textX = center.x + textRadius * sin(angleRad).toFloat()
                            val textY = center.y - textRadius * cos(angleRad).toFloat() + 4.dp.toPx()

                            val label = when (degree) {
                                0 -> "U"
                                90 -> "T"
                                180 -> "S"
                                270 -> "B"
                                else -> ""
                            }

                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = if (degree == 0) android.graphics.Color.rgb(234, 88, 12) else android.graphics.Color.DKGRAY
                                    textSize = 34f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                                drawText(label, textX, textY, paint)
                            }
                        }
                    }

                    // Needle: North (Orange) & South (Dark Slate)
                    val needleLength = radius - 30.dp.toPx()
                    val needleWidth = 10.dp.toPx()

                    // North Needle (points up relative to compass rotation)
                    val northPath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x - needleWidth, center.y)
                        lineTo(center.x + needleWidth, center.y)
                        close()
                    }
                    drawPath(
                        path = northPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFEA580C), Color(0xFFF97316))
                        )
                    )

                    // South Needle
                    val southPath = Path().apply {
                        moveTo(center.x, center.y + needleLength)
                        lineTo(center.x - needleWidth, center.y)
                        lineTo(center.x + needleWidth, center.y)
                        close()
                    }
                    drawPath(
                        path = southPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF64748B), Color(0xFF334155))
                        )
                    )

                    // Center pivot pin
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFFEA580C),
                        radius = 3.5.dp.toPx(),
                        center = center
                    )
                }

                // Evacuation Indicator Pill
                Surface(
                    color = Color(0xFF16A34A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Arah Evakuasi Tsunami: Timur (65°)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Evacuation Destination Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF16A34A).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Titik Kumpul Aman Terdekat",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${state.evacuationPointName} (~${state.distanceKmToEvacuationPoint} km)",
                                fontSize = 11.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Pedoman Evakuasi Cepat: Bergeraklah menjauhi bibir pantai Samudra Hindia menuju dataran tinggi ke arah Timur Laut. Hindari jembatan sempit dan ikuti rambu jalur evakuasi BPBD.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenEvacuationMap()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_compass_open_map")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Peta Evakuasi", fontSize = 12.sp)
                }

                Button(
                    onClick = onCall117,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_compass_call_117"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call 117", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
