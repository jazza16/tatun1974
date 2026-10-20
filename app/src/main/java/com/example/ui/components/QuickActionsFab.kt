package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary

@Composable
fun QuickActionsFab(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectMaritimeGuide: () -> Unit,
    onSelectEvacuationProcedures: () -> Unit,
    onSelectDisasterMap: () -> Unit,
    onCall117: () -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "fab_rotation"
    )

    Column(
        modifier = modifier.testTag("quick_actions_fab_container"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expanded Menu Actions
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(180)),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(150))
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Call Center 117 Action
                QuickActionMenuItem(
                    label = "Darurat Bebas Pulsa 117",
                    icon = Icons.Default.PhoneInTalk,
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    onClick = {
                        onToggleExpand()
                        onCall117()
                    },
                    testTag = "action_dial_117"
                )

                // 2. SOP Evakuasi Mandiri Action
                QuickActionMenuItem(
                    label = "SOP Evakuasi Gempa & Tsunami",
                    icon = Icons.Default.Shield,
                    containerColor = Color(0xFFEA580C),
                    contentColor = Color.White,
                    onClick = {
                        onToggleExpand()
                        onSelectEvacuationProcedures()
                    },
                    testTag = "action_evacuation_procedures"
                )

                // 3. Maritime Safety Guide Action
                QuickActionMenuItem(
                    label = "Panduan Keselamatan Maritim",
                    icon = Icons.Default.Surfing,
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    onClick = {
                        onToggleExpand()
                        onSelectMaritimeGuide()
                    },
                    testTag = "action_maritime_safety_guide"
                )

                // 4. Interactive Map Action
                QuickActionMenuItem(
                    label = "Peta Bencana & Rute Mukomuko",
                    icon = Icons.Default.Map,
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White,
                    onClick = {
                        onToggleExpand()
                        onSelectDisasterMap()
                    },
                    testTag = "action_disaster_map"
                )
            }
        }

        // Main FAB button
        FloatingActionButton(
            onClick = onToggleExpand,
            containerColor = OrangePrimary,
            contentColor = OrangeOnPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            shape = CircleShape,
            modifier = Modifier.testTag("main_quick_actions_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "Tutup Menu Aksi Cepat" else "Buka Menu Aksi Cepat",
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation)
                )
                if (!isExpanded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aksi Cepat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionMenuItem(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, containerColor.copy(alpha = 0.3f))
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            modifier = Modifier.testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
