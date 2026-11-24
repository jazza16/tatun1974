package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Tombol Slider Standar SIPAGAR KITA — mengikuti bentuk ramping & fungsi geser (tap + slide)
 * tombol SwitchMaterial pada halaman Form Laporan Harian Pusdalops:
 * - Track ramping berbentuk kapsul (36.dp × 14.dp) dengan warna transparan saat aktif
 * - Thumb bulat melayang (20.dp) dengan bayangan lembut yang menonjol halus dari track
 * - Mendukung ketuk (tap) maupun geser horizontal (drag/slide) dengan animasi pegas responsif
 * - Batas layout presisi (42.dp × 24.dp) agar tidak pernah menabrak atau menumpuk teks di sebelahnya
 */
@Composable
fun SipagarSliderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compactTouchTarget: Boolean = false,
    checkedThumbColor: Color = Color(0xFFFF9800),
    checkedTrackColor: Color = checkedThumbColor.copy(alpha = 0.45f),
    uncheckedThumbColor: Color = Color(0xFFFAFAFA),
    uncheckedTrackColor: Color = Color(0xFFBDBDBD)
) {
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentEnabled by rememberUpdatedState(enabled)

    val trackWidth = 36.dp
    val trackHeight = 14.dp
    val thumbSize = 20.dp
    val containerWidth = 42.dp
    val containerHeight = 24.dp

    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) containerWidth - thumbSize - 1.dp else 1.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sipagar_slider_thumb_x"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sipagar_slider_track_color"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) checkedThumbColor else uncheckedThumbColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sipagar_slider_thumb_color"
    )

    val interactionSource = remember { MutableInteractionSource() }
    var dragAccumulatedPx by remember { mutableFloatStateOf(0f) }

    val baseModifier = if (compactTouchTarget) {
        modifier
    } else {
        modifier.minimumInteractiveComponentSize()
    }

    Box(
        modifier = baseModifier
            .size(width = containerWidth, height = containerHeight)
            .alpha(if (enabled) 1f else 0.48f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 20.dp),
                onValueChange = onCheckedChange
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulatedPx = 0f },
                    onDragEnd = { dragAccumulatedPx = 0f },
                    onDragCancel = { dragAccumulatedPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!currentEnabled) return@detectHorizontalDragGestures
                        change.consume()
                        dragAccumulatedPx += dragAmount
                        val thresholdPx = 18f
                        if (dragAccumulatedPx > thresholdPx && !currentChecked) {
                            currentOnCheckedChange(true)
                            dragAccumulatedPx = 0f
                        } else if (dragAccumulatedPx < -thresholdPx && currentChecked) {
                            currentOnCheckedChange(false)
                            dragAccumulatedPx = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track kapsul ramping terpusat (seragam SwitchMaterial Laporan Harian Pusdalops)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        )

        // Thumb bulat melayang dengan elevasi lembut
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .size(thumbSize)
                .shadow(elevation = 2.5.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(thumbColor)
                .border(
                    width = 0.5.dp,
                    color = if (checked) checkedThumbColor.copy(alpha = 0.25f) else Color(0x26000000),
                    shape = CircleShape
                )
        )
    }
}
