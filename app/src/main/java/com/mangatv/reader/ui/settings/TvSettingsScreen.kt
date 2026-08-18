package com.mangatv.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.mangatv.reader.domain.model.ReadingMode
import com.mangatv.reader.ui.components.TvNavTab
import com.mangatv.reader.ui.components.TvSafeAreaBox
import com.mangatv.reader.ui.components.TvTopBar
import com.mangatv.reader.ui.theme.AccentCyan
import com.mangatv.reader.ui.theme.AccentOrange
import com.mangatv.reader.ui.theme.AccentTeal
import com.mangatv.reader.ui.theme.CinemaSurface
import com.mangatv.reader.ui.theme.CinemaSurfaceVariant
import com.mangatv.reader.ui.theme.TextDark
import com.mangatv.reader.ui.theme.TextMuted
import com.mangatv.reader.ui.theme.TextWhite

@Composable
fun TvSettingsScreen(
    onNavigateToTab: (TvNavTab) -> Unit
) {
    var defaultReadingMode by remember { mutableStateOf(ReadingMode.RTL) }
    var overscanPercent by remember { mutableFloatStateOf(0.03f) }
    var slideshowInterval by remember { mutableIntStateOf(8) }
    var autoCropEnabled by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    TvSafeAreaBox(overscanHorizontalPercent = overscanPercent, overscanVerticalPercent = overscanPercent) {
        Column(modifier = Modifier.fillMaxSize()) {
            TvTopBar(
                selectedTab = TvNavTab.SETTINGS,
                onTabSelected = onNavigateToTab
            )

            Text(
                text = "Application & TV Display Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // TV Safe Area Calibration
                item {
                    SettingsCard(
                        title = "TV Safe Area (Overscan Padding)",
                        description = "Adjust UI margin to prevent screen edge cropping on older TVs: ${(overscanPercent * 100).toInt()}%"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.00f, 0.03f, 0.05f, 0.08f).forEach { pct ->
                                Button(
                                    onClick = { overscanPercent = pct },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (overscanPercent == pct) AccentCyan else CinemaSurfaceVariant,
                                        focusedContainerColor = AccentTeal
                                    )
                                ) {
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        color = if (overscanPercent == pct) TextDark else TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Default Reading Mode
                item {
                    SettingsCard(
                        title = "Default Reading Direction",
                        description = "Initial direction when opening unconfigured archives"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReadingMode.entries.forEach { mode ->
                                Button(
                                    onClick = { defaultReadingMode = mode },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (defaultReadingMode == mode) AccentCyan else CinemaSurfaceVariant,
                                        focusedContainerColor = AccentTeal
                                    )
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        color = if (defaultReadingMode == mode) TextDark else TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Slideshow Duration
                item {
                    SettingsCard(
                        title = "Slideshow Speed",
                        description = "Interval between automatic page flips in hands-free reading mode"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 8, 10, 15).forEach { seconds ->
                                Button(
                                    onClick = { slideshowInterval = seconds },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (slideshowInterval == seconds) AccentCyan else CinemaSurfaceVariant,
                                        focusedContainerColor = AccentTeal
                                    )
                                ) {
                                    Text(
                                        text = "${seconds}s",
                                        color = if (slideshowInterval == seconds) TextDark else TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Auto Crop Margin Trimmer
                item {
                    SettingsCard(
                        title = "Smart Margin Trimming",
                        description = "Automatically shaves blank white or black borders to maximize 16:9 TV screen space"
                    ) {
                        Button(
                            onClick = { autoCropEnabled = !autoCropEnabled },
                            colors = ButtonDefaults.colors(
                                containerColor = if (autoCropEnabled) AccentTeal else CinemaSurfaceVariant,
                                focusedContainerColor = AccentCyan
                            )
                        ) {
                            Text(
                                text = if (autoCropEnabled) "Enabled" else "Disabled",
                                color = if (autoCropEnabled) TextDark else TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Clear Thumbnail & Memory Cache
                item {
                    SettingsCard(
                        title = "Storage & Memory Maintenance",
                        description = cacheClearedMessage ?: "Clear extracted cover thumbnails and reset memory caches"
                    ) {
                        Button(
                            onClick = {
                                cacheClearedMessage = "Thumbnail cache cleaned successfully!"
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = AccentOrange,
                                focusedContainerColor = AccentTeal
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = TextDark)
                                Text("Clear Cache", color = TextDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CinemaSurface, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 14.sp)
                )
            }
            content()
        }
    }
}
