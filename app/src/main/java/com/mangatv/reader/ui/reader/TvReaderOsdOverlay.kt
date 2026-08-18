package com.mangatv.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.mangatv.reader.domain.model.AspectRatioMode
import com.mangatv.reader.domain.model.ReadingMode
import com.mangatv.reader.ui.theme.AccentCyan
import com.mangatv.reader.ui.theme.AccentOrange
import com.mangatv.reader.ui.theme.AccentTeal
import com.mangatv.reader.ui.theme.CinemaCardBg
import com.mangatv.reader.ui.theme.CinemaSurfaceVariant
import com.mangatv.reader.ui.theme.OsdBackground
import com.mangatv.reader.ui.theme.TextDark
import com.mangatv.reader.ui.theme.TextMuted
import com.mangatv.reader.ui.theme.TextWhite

import androidx.compose.material.icons.filled.AutoStories

@Composable
fun TvReaderOsdOverlay(
    isVisible: Boolean,
    title: String,
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    aspectRatioMode: AspectRatioMode,
    spreadMode: com.mangatv.reader.domain.model.PageSpreadMode = com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE,
    isDualSpread: Boolean = false,
    zoomScale: Float = 1.0f,
    isAutoCrop: Boolean,
    isSlideshow: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onJumpTenBack: () -> Unit,
    onJumpTenForward: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onCycleZoom: () -> Unit = {},
    onToggleSpreadMode: () -> Unit,
    onToggleAutoCrop: () -> Unit,
    onToggleSlideshow: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialFocusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(OsdBackground)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.colors(
                            containerColor = CinemaSurfaceVariant,
                            focusedContainerColor = AccentCyan
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Exit to Library", tint = TextWhite)
                            Text("Library", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                }

                // Page count indicator
                Box(
                    modifier = Modifier
                        .background(AccentCyan, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val pageText = if (isDualSpread && currentPage + 1 < totalPages) {
                        "Page ${currentPage + 1}-${currentPage + 2} of $totalPages"
                    } else {
                        "Page ${currentPage + 1} of $totalPages"
                    }
                    Text(
                        text = pageText,
                        style = MaterialTheme.typography.labelLarge.copy(color = TextDark, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Bottom Control Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(OsdBackground)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek & Fast Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OsdActionButton(
                        title = "-10",
                        icon = Icons.Default.FastRewind,
                        onClick = onJumpTenBack
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OsdActionButton(
                        title = "Prev",
                        icon = Icons.Default.MenuBook,
                        onClick = onPrevPage
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    // Scrubber visual
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .height(10.dp)
                            .background(CinemaSurfaceVariant, RoundedCornerShape(5.dp))
                    ) {
                        val progressFraction = if (totalPages > 0) (currentPage + 1).toFloat() / totalPages else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .height(10.dp)
                                .background(AccentCyan, RoundedCornerShape(5.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    OsdActionButton(
                        title = "Next",
                        icon = Icons.Default.MenuBook,
                        modifier = if (initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier,
                        onClick = onNextPage
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OsdActionButton(
                        title = "+10",
                        icon = Icons.Default.FastForward,
                        onClick = onJumpTenForward
                    )
                }

                // Feature Controls: Mode / Spread / Aspect / Auto-Crop / Slideshow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OsdActionButton(
                        title = readingMode.displayName,
                        icon = Icons.Default.SwapHoriz,
                        isActive = true,
                        onClick = onToggleReadingMode
                    )
                    OsdActionButton(
                        title = if (spreadMode == com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE) "Dual Spread" else "Single Page",
                        icon = Icons.Default.AutoStories,
                        isActive = (spreadMode == com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE),
                        onClick = onToggleSpreadMode
                    )
                    OsdActionButton(
                        title = aspectRatioMode.displayName,
                        icon = Icons.Default.AspectRatio,
                        isActive = true,
                        onClick = onCycleAspectRatio
                    )
                    OsdActionButton(
                        title = if (zoomScale == 1.0f) "Zoom: 1.0x" else "Zoom: ${"%.2f".format(zoomScale).trimEnd('0').trimEnd('.')}x",
                        icon = Icons.Default.ZoomIn,
                        isActive = zoomScale > 1.0f,
                        onClick = onCycleZoom
                    )
                    OsdActionButton(
                        title = if (isAutoCrop) "Auto-Crop: ON" else "Auto-Crop: OFF",
                        icon = Icons.Default.Crop,
                        isActive = isAutoCrop,
                        onClick = onToggleAutoCrop
                    )
                    OsdActionButton(
                        title = if (isSlideshow) "Slideshow: ON" else "Slideshow: OFF",
                        icon = if (isSlideshow) Icons.Default.Pause else Icons.Default.PlayArrow,
                        isActive = isSlideshow,
                        onClick = onToggleSlideshow
                    )
                }
            }
        }
    }
}

@Composable
private fun OsdActionButton(
    title: String,
    icon: ImageVector,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) AccentCyan else Color(0xFF30363D),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isActive) CinemaSurfaceVariant else CinemaCardBg,
            focusedContainerColor = AccentCyan
        ),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isFocused) TextDark else if (isActive) AccentCyan else TextWhite,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    color = if (isFocused) TextDark else TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
