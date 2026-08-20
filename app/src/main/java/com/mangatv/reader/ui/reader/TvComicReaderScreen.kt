package com.mangatv.reader.ui.reader

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.mangatv.reader.domain.model.AspectRatioMode
import com.mangatv.reader.domain.model.ReadingMode
import com.mangatv.reader.ui.theme.AccentCyan
import com.mangatv.reader.ui.theme.AccentTeal
import com.mangatv.reader.ui.theme.CinemaDarkBg
import com.mangatv.reader.ui.theme.CinemaSurface
import com.mangatv.reader.ui.theme.CinemaSurfaceVariant
import com.mangatv.reader.ui.theme.TextDark
import com.mangatv.reader.ui.theme.TextMuted
import com.mangatv.reader.ui.theme.TextWhite

@Composable
fun TvComicReaderScreen(
    filePath: String,
    onBackToLibrary: () -> Unit,
    viewModel: TvComicReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val readerFocusRequester = remember { FocusRequester() }
    val osdFocusRequester = remember { FocusRequester() }
    var isZoomBadgeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.aspectMode, uiState.zoomScale, filePath) {
        isZoomBadgeVisible = true
        kotlinx.coroutines.delay(3500)
        isZoomBadgeVisible = false
    }

    LaunchedEffect(filePath) {
        viewModel.openComic(filePath)
        try {
            readerFocusRequester.requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }

    LaunchedEffect(uiState.isOsdVisible) {
        if (uiState.isOsdVisible) {
            kotlinx.coroutines.delay(150)
            try {
                osdFocusRequester.requestFocus()
            } catch (e: Exception) {
                // ignore
            }
        } else {
            try {
                readerFocusRequester.requestFocus()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    BackHandler {
        if (uiState.isOsdVisible) {
            viewModel.hideOsd()
        } else if (uiState.zoomScale > 1.0f || uiState.panOffsetX != 0f || uiState.panOffsetY != 0f) {
            viewModel.resetZoomAndPan()
        } else {
            viewModel.saveCurrentProgress()
            onBackToLibrary()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaDarkBg)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val screenAspect = screenWidthPx / screenHeightPx

        val bitmap = uiState.currentBitmap
        val secBmp = uiState.secondaryBitmap
        val isDual = uiState.isCurrentSpreadDual && secBmp != null

        val imageAspect = if (bitmap != null) {
            if (isDual && secBmp != null) {
                val totalWidth = bitmap.width.toFloat() + secBmp.width.toFloat()
                val maxHeight = maxOf(bitmap.height.toFloat(), secBmp.height.toFloat()).coerceAtLeast(1f)
                totalWidth / maxHeight
            } else {
                bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
            }
        } else {
            screenAspect
        }

        val zoom = uiState.zoomScale
        val (baseContentWidth, baseContentHeight) = when (uiState.aspectMode) {
            AspectRatioMode.FIT_SCREEN -> {
                if (imageAspect > screenAspect) {
                    screenWidthPx to (screenWidthPx / imageAspect)
                } else {
                    (screenHeightPx * imageAspect) to screenHeightPx
                }
            }
            AspectRatioMode.FIT_WIDTH -> {
                screenWidthPx to (screenWidthPx / imageAspect)
            }
            AspectRatioMode.FIT_HEIGHT -> {
                (screenHeightPx * imageAspect) to screenHeightPx
            }
            AspectRatioMode.ORIGINAL -> {
                val origW = if (isDual && secBmp != null) bitmap!!.width.toFloat() + secBmp.width.toFloat() else (bitmap?.width?.toFloat() ?: screenWidthPx)
                val origH = if (isDual && secBmp != null) maxOf(bitmap!!.height.toFloat(), secBmp.height.toFloat()) else (bitmap?.height?.toFloat() ?: screenHeightPx)
                origW to origH
            }
            AspectRatioMode.STRETCH -> {
                screenWidthPx to screenHeightPx
            }
        }

        val totalContentWidth = baseContentWidth * zoom
        val totalContentHeight = baseContentHeight * zoom

        val maxPanX = ((totalContentWidth - screenWidthPx) / 2f).coerceAtLeast(0f)
        val maxPanY = ((totalContentHeight - screenHeightPx) / 2f).coerceAtLeast(0f)

        val scrollStepY = screenHeightPx * 0.30f
        val scrollStepX = screenWidthPx * 0.30f

        val targetPanX = uiState.panOffsetX.coerceIn(-maxPanX, maxPanX)
        val targetPanY = uiState.panOffsetY.coerceIn(-maxPanY, maxPanY)

        // Smooth animated pan and zoom values with immediate page-turn snap
        val animatedPanX by key(uiState.currentPageIndex) {
            animateFloatAsState(
                targetValue = targetPanX,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "panX"
            )
        }
        val animatedPanY by key(uiState.currentPageIndex) {
            animateFloatAsState(
                targetValue = targetPanY,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "panY"
            )
        }
        val animatedZoom by animateFloatAsState(
            targetValue = uiState.zoomScale,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "zoom"
        )

        val density = LocalDensity.current
        val contentWidthDp = with(density) { baseContentWidth.toDp() }
        val contentHeightDp = with(density) { baseContentHeight.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(readerFocusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
                        return@onKeyEvent false
                    }

                    if (uiState.isOsdVisible) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_MENU -> {
                                viewModel.hideOsd()
                                true
                            }
                            else -> false
                        }
                    } else {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                if (maxPanY > 10f) {
                                    viewModel.panVertical(scrollStepY, maxPanY)
                                    true
                                } else {
                                    false
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (maxPanY > 10f) {
                                    viewModel.panVertical(-scrollStepY, maxPanY)
                                    true
                                } else {
                                    false
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (maxPanX > 10f) {
                                    if (uiState.readingMode == ReadingMode.RTL) {
                                        if (uiState.panOffsetX < maxPanX - 15f) {
                                            viewModel.panHorizontal(scrollStepX, maxPanX)
                                        } else {
                                            viewModel.nextPage()
                                        }
                                    } else {
                                        if (uiState.panOffsetX < maxPanX - 15f) {
                                            viewModel.panHorizontal(scrollStepX, maxPanX)
                                        } else {
                                            viewModel.prevPage()
                                        }
                                    }
                                    true
                                } else {
                                    if (uiState.readingMode == ReadingMode.RTL) {
                                        viewModel.nextPage()
                                    } else {
                                        viewModel.prevPage()
                                    }
                                    true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (maxPanX > 10f) {
                                    if (uiState.readingMode == ReadingMode.RTL) {
                                        if (uiState.panOffsetX > -maxPanX + 15f) {
                                            viewModel.panHorizontal(-scrollStepX, maxPanX)
                                        } else {
                                            viewModel.prevPage()
                                        }
                                    } else {
                                        if (uiState.panOffsetX > -maxPanX + 15f) {
                                            viewModel.panHorizontal(-scrollStepX, maxPanX)
                                        } else {
                                            viewModel.nextPage()
                                        }
                                    }
                                    true
                                } else {
                                    if (uiState.readingMode == ReadingMode.RTL) {
                                        viewModel.prevPage()
                                    } else {
                                        viewModel.nextPage()
                                    }
                                    true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_MENU,
                            KeyEvent.KEYCODE_SPACE -> {
                                viewModel.toggleOsd()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                viewModel.toggleSlideshow()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                            KeyEvent.KEYCODE_PAGE_DOWN -> {
                                viewModel.jumpPages(10)
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND,
                            KeyEvent.KEYCODE_PAGE_UP -> {
                                viewModel.jumpPages(-10)
                                true
                            }
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> {
                                if (uiState.zoomScale > 1.0f || uiState.panOffsetX != 0f || uiState.panOffsetY != 0f) {
                                    viewModel.resetZoomAndPan()
                                    true
                                } else {
                                    viewModel.saveCurrentProgress()
                                    onBackToLibrary()
                                    true
                                }
                            }
                            else -> false
                        }
                    }
                }
        ) {
            // Render Comic Page Image Container (instant, zero-latency, full unclipped canvas)
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(contentWidthDp, contentHeightDp)
                            .graphicsLayer {
                                scaleX = animatedZoom
                                scaleY = animatedZoom
                                translationX = animatedPanX
                                translationY = animatedPanY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDual && secBmp != null) {
                            val (leftBmp, rightBmp) = if (uiState.readingMode == ReadingMode.RTL) {
                                secBmp to bitmap
                            } else {
                                bitmap to secBmp
                            }
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = leftBmp.asImageBitmap(),
                                    contentDescription = "Left Page",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                Image(
                                    bitmap = rightBmp.asImageBitmap(),
                                    contentDescription = "Right Page",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        } else {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page ${uiState.currentPageIndex + 1}",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            }

            // Viewport / Zoom & Panning Position Badge Indicator (Auto-dismisses after 3.5s)
            AnimatedVisibility(
                visible = isZoomBadgeVisible && (maxPanX > 10f || maxPanY > 10f || uiState.zoomScale > 1.0f),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(500)),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp, end = 24.dp)
                        .background(Color(0xAA111827), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val zoomText = if (uiState.zoomScale > 1.0f) "${"%.2f".format(uiState.zoomScale).trimEnd('0').trimEnd('.')}x" else uiState.aspectMode.displayName
                    Text(
                        text = "🔍 $zoomText • D-Pad to Pan",
                        style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // OLED Burn-in screen dimming overlay
            AnimatedVisibility(
                visible = uiState.isOledDimmed,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OLED Sleep Mode • Press any key to resume",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                    )
                }
            }

            // End of chapter / Next Volume prompt
            AnimatedVisibility(
                visible = uiState.isAtEndPromptVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(CinemaSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = AccentCyan
                        )
                        Column {
                            Text(
                                text = "End of Chapter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (uiState.nextVolumePath != null) "Next volume available" else "You've finished this archive",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                            )
                        }

                        if (uiState.nextVolumePath != null) {
                            Button(
                                onClick = {
                                    uiState.nextVolumePath?.let { next ->
                                        viewModel.openComic(next)
                                    }
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = AccentCyan,
                                    focusedContainerColor = AccentTeal
                                )
                            ) {
                                Text("Open Next Volume", color = TextDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveCurrentProgress()
                                onBackToLibrary()
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = CinemaSurfaceVariant,
                                focusedContainerColor = AccentCyan
                            )
                        ) {
                            Text("Exit to Library", color = TextWhite)
                        }
                    }
                }
            }

            // On-Screen Display (OSD) Overlay
            TvReaderOsdOverlay(
                isVisible = uiState.isOsdVisible,
                title = uiState.title,
                currentPage = uiState.currentPageIndex,
                totalPages = uiState.totalPages,
                readingMode = uiState.readingMode,
                aspectRatioMode = uiState.aspectMode,
                spreadMode = uiState.spreadMode,
                isDualSpread = uiState.isCurrentSpreadDual,
                zoomScale = uiState.zoomScale,
                isAutoCrop = uiState.isAutoCropEnabled,
                isSlideshow = uiState.isSlideshowActive,
                onPrevPage = { viewModel.prevPage() },
                onNextPage = { viewModel.nextPage() },
                onJumpTenBack = { viewModel.jumpPages(-10) },
                onJumpTenForward = { viewModel.jumpPages(10) },
                onToggleReadingMode = {
                    val nextMode = when (uiState.readingMode) {
                        ReadingMode.RTL -> ReadingMode.LTR
                        ReadingMode.LTR -> ReadingMode.WEBTOON
                        ReadingMode.WEBTOON -> ReadingMode.RTL
                    }
                    viewModel.setReadingMode(nextMode)
                },
                onCycleAspectRatio = {
                    val nextAspect = when (uiState.aspectMode) {
                        AspectRatioMode.FIT_SCREEN -> AspectRatioMode.FIT_WIDTH
                        AspectRatioMode.FIT_WIDTH -> AspectRatioMode.FIT_HEIGHT
                        AspectRatioMode.FIT_HEIGHT -> AspectRatioMode.ORIGINAL
                        AspectRatioMode.ORIGINAL -> AspectRatioMode.STRETCH
                        AspectRatioMode.STRETCH -> AspectRatioMode.FIT_SCREEN
                    }
                    viewModel.setAspectRatio(nextAspect)
                },
                onCycleZoom = {
                    val nextZoom = when {
                        uiState.zoomScale < 1.25f -> 1.25f
                        uiState.zoomScale < 1.5f -> 1.5f
                        uiState.zoomScale < 2.0f -> 2.0f
                        uiState.zoomScale < 3.0f -> 3.0f
                        else -> 1.0f
                    }
                    viewModel.setZoomScale(nextZoom)
                },
                onToggleSpreadMode = { viewModel.togglePageSpreadMode() },
                onToggleAutoCrop = { viewModel.toggleAutoCrop() },
                onToggleSlideshow = { viewModel.toggleSlideshow() },
                onBack = {
                    viewModel.saveCurrentProgress()
                    onBackToLibrary()
                },
                initialFocusRequester = osdFocusRequester
            )
        }
    }
}
