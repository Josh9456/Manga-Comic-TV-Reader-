package com.mangatv.reader.ui.reader

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

@Composable
fun TvComicReaderScreen(
    filePath: String,
    onBackToLibrary: () -> Unit,
    viewModel: TvComicReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val readerFocusRequester = remember { FocusRequester() }
    val osdFocusRequester = remember { FocusRequester() }

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
        } else {
            viewModel.saveCurrentProgress()
            onBackToLibrary()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaDarkBg)
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
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (uiState.readingMode == ReadingMode.RTL) {
                                viewModel.nextPage()
                            } else {
                                viewModel.prevPage()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (uiState.readingMode == ReadingMode.RTL) {
                                viewModel.prevPage()
                            } else {
                                viewModel.nextPage()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            viewModel.toggleOsd()
                            true
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
                            viewModel.saveCurrentProgress()
                            onBackToLibrary()
                            true
                        }
                        else -> false
                    }
                }
            }
    ) {
        // Render current Comic page bitmap
        val bitmap = uiState.currentBitmap
        if (bitmap != null) {
            val contentScale = when (uiState.aspectMode) {
                AspectRatioMode.FIT_SCREEN -> ContentScale.Fit
                AspectRatioMode.FIT_WIDTH -> ContentScale.FillWidth
                AspectRatioMode.FIT_HEIGHT -> ContentScale.FillHeight
                AspectRatioMode.ORIGINAL -> ContentScale.None
                AspectRatioMode.STRETCH -> ContentScale.FillBounds
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            uiState.panOffsetX.roundToInt(),
                            uiState.panOffsetY.roundToInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val secBmp = uiState.secondaryBitmap
                if (uiState.isCurrentSpreadDual && secBmp != null) {
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
                            contentScale = contentScale,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Image(
                            bitmap = rightBmp.asImageBitmap(),
                            contentDescription = "Right Page",
                            contentScale = contentScale,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page ${uiState.currentPageIndex + 1}",
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize()
                    )
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
