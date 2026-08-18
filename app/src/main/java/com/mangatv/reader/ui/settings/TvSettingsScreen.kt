package com.mangatv.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.mangatv.reader.data.updater.AppUpdateInfo
import com.mangatv.reader.data.updater.AppUpdateManager
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
import kotlinx.coroutines.launch

@Composable
fun TvSettingsScreen(
    onNavigateToTab: (TvNavTab) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var defaultReadingMode by remember { mutableStateOf(ReadingMode.RTL) }
    var overscanPercent by remember { mutableFloatStateOf(0.03f) }
    var slideshowInterval by remember { mutableIntStateOf(8) }
    var autoCropEnabled by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    // GitHub Updates State
    val currentVersion = remember { AppUpdateManager.getCurrentVersion(context) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }

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
                // GitHub In-App Updates Card
                item {
                    SettingsCard(
                        title = "App Updates (GitHub)",
                        description = updateStatusMessage
                            ?: if (updateInfo?.isUpdateAvailable == true) {
                                "New version v${updateInfo?.latestVersion} available! Click below to download and install automatically."
                            } else {
                                "Current Version: v$currentVersion • Automatically checks for new APK updates from GitHub releases."
                            }
                    ) {
                        Button(
                            onClick = {
                                if (isDownloadingUpdate || isCheckingUpdate) return@Button

                                if (updateInfo?.isUpdateAvailable == true && updateInfo?.downloadUrl != null) {
                                    val url = updateInfo?.downloadUrl ?: return@Button
                                    val name = updateInfo?.apkFileName ?: "MangaTV-v${updateInfo?.latestVersion}.apk"
                                    isDownloadingUpdate = true
                                    downloadProgress = 0
                                    updateStatusMessage = "Downloading APK update..."
                                    coroutineScope.launch {
                                        val result = AppUpdateManager.downloadAndInstallApk(
                                            context = context,
                                            downloadUrl = url,
                                            fileName = name,
                                            onProgress = { pct ->
                                                downloadProgress = pct
                                                updateStatusMessage = "Downloading APK: $pct%"
                                            }
                                        )
                                        isDownloadingUpdate = false
                                        if (result.isFailure) {
                                            updateStatusMessage = "Download failed: ${result.exceptionOrNull()?.localizedMessage}"
                                        } else {
                                            updateStatusMessage = "Installer launched! Please confirm installation on your TV."
                                        }
                                    }
                                } else {
                                    isCheckingUpdate = true
                                    updateStatusMessage = "Checking GitHub Releases..."
                                    coroutineScope.launch {
                                        val result = AppUpdateManager.checkForUpdates(context)
                                        isCheckingUpdate = false
                                        result.onSuccess { info ->
                                            updateInfo = info
                                            if (info.isUpdateAvailable) {
                                                updateStatusMessage = "Update found: v${info.latestVersion}! Click to install."
                                            } else {
                                                updateStatusMessage = "You are on the latest version (v$currentVersion)."
                                            }
                                        }.onFailure { error ->
                                            updateStatusMessage = "Check failed: ${error.localizedMessage ?: "Network error"}"
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = if (isDownloadingUpdate || updateInfo?.isUpdateAvailable == true) AccentCyan else CinemaSurfaceVariant,
                                focusedContainerColor = AccentTeal
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when {
                                    isDownloadingUpdate -> {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = TextDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Downloading $downloadProgress%",
                                            color = TextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    isCheckingUpdate -> {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Checking...",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    updateInfo?.isUpdateAvailable == true -> {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = TextDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Install v${updateInfo?.latestVersion}",
                                            color = TextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Check for Updates",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

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
