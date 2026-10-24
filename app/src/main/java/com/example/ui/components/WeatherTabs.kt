package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary

data class TabItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int? = null,
    val isWarningBadge: Boolean = false
)

@Composable
fun WeatherTabs(
    selectedTabIndex: Int,
    warningCount: Int = 0,
    reportCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabItem("Hari Ini", Icons.Default.WbSunny),
        TabItem("Prakiraan Cuaca", Icons.Default.CalendarMonth),
        TabItem("Peringatan & Gempa WRS", Icons.Default.Campaign, badgeCount = if (warningCount > 0) warningCount else null, isWarningBadge = true),
        TabItem("Peta & SOP Evakuasi", Icons.Default.Map),
        TabItem("inaRISK BNPB", Icons.Default.Security),
        TabItem("Cuaca Historis", Icons.Default.History),
        TabItem("Maritim & Pesisir", Icons.Default.Surfing),
        TabItem("Kualitas Udara", Icons.Default.Air),
        TabItem("15 Kecamatan", Icons.Default.LocationCity)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = OrangePrimary,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = OrangePrimary
                    )
                }
            },
            divider = {},
            modifier = modifier
                .fillMaxWidth()
                .testTag("weather_tab_row")
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("tab_item_$index"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(17.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            if (tab.badgeCount != null) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            if (tab.isWarningBadge) Color(0xFFDC2626) else OrangePrimary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${tab.badgeCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
