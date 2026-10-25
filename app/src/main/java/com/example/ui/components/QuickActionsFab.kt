package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Preset posisi letak Aksi Cepat pada layar.
 */
enum class QuickActionPresetPosition(val label: String, val shortCode: String) {
    BOTTOM_RIGHT("Kanan Bawah", "↘ Kanan Bwh"),
    BOTTOM_LEFT("Kiri Bawah", "↙ Kiri Bwh"),
    CENTER_RIGHT("Kanan Tengah", "➡ Kanan Tgh"),
    CENTER_LEFT("Kiri Tengah", "⬅ Kiri Tgh")
}

data class QuickActionItemData(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color = Color.White,
    val testTag: String
)

@Composable
fun QuickActionsFab(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectMaritimeGuide: () -> Unit,
    onSelectEvacuationProcedures: () -> Unit,
    onSelectDisasterMap: () -> Unit,
    onCall117: () -> Unit,
    onOpenCompass: () -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    parentMaxWidth: Dp = 380.dp,
    parentMaxHeight: Dp = 700.dp,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Action items list with reordering support
    val actionItems = remember {
        mutableStateListOf(
            QuickActionItemData("dial_117", "Darurat 117 BPBD", Icons.Default.PhoneInTalk, Color(0xFFDC2626), Color.White, "action_dial_117"),
            QuickActionItemData("compass", "Kompas Evakuasi", Icons.Default.Explore, Color(0xFF16A34A), Color.White, "action_open_compass"),
            QuickActionItemData("disaster_map", "Peta Bencana Mukomuko", Icons.Default.Map, Color(0xFF0D9488), Color.White, "action_disaster_map"),
            QuickActionItemData("evacuation", "SOP Evakuasi Gempa", Icons.Default.Shield, Color(0xFFEA580C), Color.White, "action_evacuation_procedures"),
            QuickActionItemData("maritime", "Keselamatan Maritim", Icons.Default.Surfing, Color(0xFF0284C7), Color.White, "action_maritime_safety_guide"),
            QuickActionItemData("onboarding", "Panduan Aplikasi", Icons.Default.AutoAwesome, Color(0xFF8B5CF6), Color.White, "action_open_onboarding")
        )
    }

    var isReorderMode by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(QuickActionPresetPosition.BOTTOM_RIGHT) }

    // Coordinates of the FAB
    val fabSizePx = with(density) { 48.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }
    val maxPxX = with(density) { (parentMaxWidth).toPx() } - fabSizePx - marginPx
    val maxPxY = with(density) { (parentMaxHeight).toPx() } - fabSizePx - marginPx

    var offsetX by remember { mutableFloatStateOf(maxPxX) }
    var offsetY by remember { mutableFloatStateOf(maxPxY) }
    var isDragging by remember { mutableStateOf(false) }

    // Keep clamped inside viewport if bounds change
    LaunchedEffect(maxPxX, maxPxY) {
        if (maxPxX > marginPx && maxPxY > marginPx) {
            when (selectedPreset) {
                QuickActionPresetPosition.BOTTOM_RIGHT -> {
                    offsetX = maxPxX
                    offsetY = maxPxY
                }
                QuickActionPresetPosition.BOTTOM_LEFT -> {
                    offsetX = marginPx
                    offsetY = maxPxY
                }
                QuickActionPresetPosition.CENTER_RIGHT -> {
                    offsetX = maxPxX
                    offsetY = maxPxY * 0.55f
                }
                QuickActionPresetPosition.CENTER_LEFT -> {
                    offsetX = marginPx
                    offsetY = maxPxY * 0.55f
                }
            }
        }
    }

    val isDockedLeft = offsetX < (maxPxX / 2)
    val isDockedTop = offsetY < (maxPxY / 2)

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "fab_rotation"
    )

    val fabScale by animateFloatAsState(
        targetValue = if (isDragging) 1.12f else 1f,
        animationSpec = spring(),
        label = "fab_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("quick_actions_fab_container")
    ) {
        // Scrim backdrop when menu is open to tap-to-dismiss
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .clickable {
                        isReorderMode = false
                        onToggleExpand()
                    }
            )
        }

        // Draggable FAB + Action Menu Container positioned at (offsetX, offsetY)
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .padding(0.dp),
            horizontalAlignment = if (isDockedLeft) Alignment.Start else Alignment.End,
            verticalArrangement = if (isDockedTop) Arrangement.Top else Arrangement.Bottom
        ) {
            // When docked at bottom, show menu items ABOVE the FAB
            if (!isDockedTop) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(180)),
                    exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(150))
                ) {
                    Column(
                        horizontalAlignment = if (isDockedLeft) Alignment.Start else Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        // Position Switcher & Customizer Bar
                        PositionAndReorderBar(
                            isDockedLeft = isDockedLeft,
                            isReorderMode = isReorderMode,
                            onToggleReorder = { isReorderMode = !isReorderMode },
                            onSelectPreset = { preset ->
                                selectedPreset = preset
                                when (preset) {
                                    QuickActionPresetPosition.BOTTOM_RIGHT -> {
                                        offsetX = maxPxX
                                        offsetY = maxPxY
                                    }
                                    QuickActionPresetPosition.BOTTOM_LEFT -> {
                                        offsetX = marginPx
                                        offsetY = maxPxY
                                    }
                                    QuickActionPresetPosition.CENTER_RIGHT -> {
                                        offsetX = maxPxX
                                        offsetY = maxPxY * 0.55f
                                    }
                                    QuickActionPresetPosition.CENTER_LEFT -> {
                                        offsetX = marginPx
                                        offsetY = maxPxY * 0.55f
                                    }
                                }
                            }
                        )

                        // Action Items Stack
                        actionItems.forEachIndexed { index, item ->
                            CompactQuickActionMenuItem(
                                item = item,
                                isDockedLeft = isDockedLeft,
                                isReorderMode = isReorderMode,
                                canMoveUp = index > 0,
                                canMoveDown = index < actionItems.size - 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val temp = actionItems[index]
                                        actionItems[index] = actionItems[index - 1]
                                        actionItems[index - 1] = temp
                                    }
                                },
                                onMoveDown = {
                                    if (index < actionItems.size - 1) {
                                        val temp = actionItems[index]
                                        actionItems[index] = actionItems[index + 1]
                                        actionItems[index + 1] = temp
                                    }
                                },
                                onClick = {
                                    if (!isReorderMode) {
                                        onToggleExpand()
                                        when (item.id) {
                                            "dial_117" -> onCall117()
                                            "compass" -> onOpenCompass()
                                            "disaster_map" -> onSelectDisasterMap()
                                            "evacuation" -> onSelectEvacuationProcedures()
                                            "maritime" -> onSelectMaritimeGuide()
                                            "onboarding" -> onOpenOnboarding()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Compact Floating Action Button (Movable & Draggable)
            var dragDistance = remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .scale(fabScale)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragDistance.floatValue = 0f
                            },
                            onDragEnd = {
                                isDragging = false
                                // If dragged only a tiny distance (< 12px), treat as tap
                                if (dragDistance.floatValue < 12f) {
                                    onToggleExpand()
                                } else {
                                    // Snap to nearest edge (Left or Right) for ergonomic resting
                                    val targetX = if (offsetX < (maxPxX / 2)) marginPx else maxPxX
                                    offsetX = targetX
                                    // Update preset state for indicator
                                    selectedPreset = if (targetX == marginPx) {
                                        if (offsetY < maxPxY * 0.5f) QuickActionPresetPosition.CENTER_LEFT else QuickActionPresetPosition.BOTTOM_LEFT
                                    } else {
                                        if (offsetY < maxPxY * 0.5f) QuickActionPresetPosition.CENTER_RIGHT else QuickActionPresetPosition.BOTTOM_RIGHT
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDistance.floatValue += sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                                offsetX = (offsetX + dragAmount.x).coerceIn(marginPx, maxPxX)
                                offsetY = (offsetY + dragAmount.y).coerceIn(marginPx, maxPxY)
                            }
                        )
                    }
                    .testTag("main_quick_actions_fab")
            ) {
                Surface(
                    shape = CircleShape,
                    color = OrangePrimary,
                    shadowElevation = if (isDragging) 8.dp else 4.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = if (isDragging) 0.9f else 0.4f)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.FlashOn,
                            contentDescription = if (isExpanded) "Tutup Menu Aksi Cepat" else "Buka Menu Aksi Cepat",
                            tint = OrangeOnPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(if (isExpanded) 0f else rotation)
                        )
                    }
                }

                // Tiny Drag Affordance Pill / Indicator
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(width = 12.dp, height = 2.5.dp)
                ) {}
            }

            // When docked at top, show menu items BELOW the FAB
            if (isDockedTop) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(180)),
                    exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(150))
                ) {
                    Column(
                        horizontalAlignment = if (isDockedLeft) Alignment.Start else Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Position Switcher & Customizer Bar
                        PositionAndReorderBar(
                            isDockedLeft = isDockedLeft,
                            isReorderMode = isReorderMode,
                            onToggleReorder = { isReorderMode = !isReorderMode },
                            onSelectPreset = { preset ->
                                selectedPreset = preset
                                when (preset) {
                                    QuickActionPresetPosition.BOTTOM_RIGHT -> {
                                        offsetX = maxPxX
                                        offsetY = maxPxY
                                    }
                                    QuickActionPresetPosition.BOTTOM_LEFT -> {
                                        offsetX = marginPx
                                        offsetY = maxPxY
                                    }
                                    QuickActionPresetPosition.CENTER_RIGHT -> {
                                        offsetX = maxPxX
                                        offsetY = maxPxY * 0.55f
                                    }
                                    QuickActionPresetPosition.CENTER_LEFT -> {
                                        offsetX = marginPx
                                        offsetY = maxPxY * 0.55f
                                    }
                                }
                            }
                        )

                        // Action Items Stack
                        actionItems.forEachIndexed { index, item ->
                            CompactQuickActionMenuItem(
                                item = item,
                                isDockedLeft = isDockedLeft,
                                isReorderMode = isReorderMode,
                                canMoveUp = index > 0,
                                canMoveDown = index < actionItems.size - 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val temp = actionItems[index]
                                        actionItems[index] = actionItems[index - 1]
                                        actionItems[index - 1] = temp
                                    }
                                },
                                onMoveDown = {
                                    if (index < actionItems.size - 1) {
                                        val temp = actionItems[index]
                                        actionItems[index] = actionItems[index + 1]
                                        actionItems[index + 1] = temp
                                    }
                                },
                                onClick = {
                                    if (!isReorderMode) {
                                        onToggleExpand()
                                        when (item.id) {
                                            "dial_117" -> onCall117()
                                            "compass" -> onOpenCompass()
                                            "disaster_map" -> onSelectDisasterMap()
                                            "evacuation" -> onSelectEvacuationProcedures()
                                            "maritime" -> onSelectMaritimeGuide()
                                            "onboarding" -> onOpenOnboarding()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact bar for moving the FAB to preset positions or reordering actions.
 */
@Composable
private fun PositionAndReorderBar(
    isDockedLeft: Boolean,
    isReorderMode: Boolean,
    onToggleReorder: () -> Unit,
    onSelectPreset: (QuickActionPresetPosition) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(210.dp)
            ) {
                Text(
                    text = "Posisi & Pindah Aksi",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )

                // Reorder mode toggle chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isReorderMode) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable(onClick = onToggleReorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isReorderMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReorderMode) "Selesai" else "Urutkan",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReorderMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick dock position pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickPositionChip("Kanan Bawah", onClick = { onSelectPreset(QuickActionPresetPosition.BOTTOM_RIGHT) })
                QuickPositionChip("Kiri Bawah", onClick = { onSelectPreset(QuickActionPresetPosition.BOTTOM_LEFT) })
                QuickPositionChip("Kanan Tgh", onClick = { onSelectPreset(QuickActionPresetPosition.CENTER_RIGHT) })
                QuickPositionChip("Kiri Tgh", onClick = { onSelectPreset(QuickActionPresetPosition.CENTER_LEFT) })
            }
        }
    }
}

@Composable
private fun QuickPositionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

/**
 * Modern compact action item pill.
 */
@Composable
private fun CompactQuickActionMenuItem(
    item: QuickActionItemData,
    isDockedLeft: Boolean,
    isReorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isDockedLeft) Arrangement.Start else Arrangement.End,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(item.testTag)
    ) {
        // If docked on Left: Action Button first, then Label
        if (isDockedLeft) {
            ActionCircleButton(item = item, onClick = onClick)
            Spacer(modifier = Modifier.width(6.dp))
            ActionLabelPill(item = item)
            if (isReorderMode) {
                Spacer(modifier = Modifier.width(4.dp))
                ReorderArrows(canMoveUp = canMoveUp, canMoveDown = canMoveDown, onMoveUp = onMoveUp, onMoveDown = onMoveDown)
            }
        } else {
            // Docked on Right: Label first, then Action Button
            if (isReorderMode) {
                ReorderArrows(canMoveUp = canMoveUp, canMoveDown = canMoveDown, onMoveUp = onMoveUp, onMoveDown = onMoveDown)
                Spacer(modifier = Modifier.width(4.dp))
            }
            ActionLabelPill(item = item)
            Spacer(modifier = Modifier.width(6.dp))
            ActionCircleButton(item = item, onClick = onClick)
        }
    }
}

@Composable
private fun ActionCircleButton(
    item: QuickActionItemData,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = item.containerColor,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ActionLabelPill(
    item: QuickActionItemData
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, item.containerColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
        )
    }
}

@Composable
private fun ReorderArrows(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (canMoveUp) OrangePrimary else Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier
                .size(20.dp)
                .clickable(enabled = canMoveUp, onClick = onMoveUp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Pindah Ke Atas",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = if (canMoveDown) OrangePrimary else Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier
                .size(20.dp)
                .clickable(enabled = canMoveDown, onClick = onMoveDown)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Pindah Ke Bawah",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
