package com.mangatv.reader.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.mangatv.reader.data.db.entity.ComicProgressEntity
import com.mangatv.reader.domain.model.ComicItem
import com.mangatv.reader.ui.components.TvFocusableCard
import com.mangatv.reader.ui.components.TvNavTab
import com.mangatv.reader.ui.components.TvProgressBar
import com.mangatv.reader.ui.components.TvReadingBadge
import com.mangatv.reader.ui.components.TvSafeAreaBox
import com.mangatv.reader.ui.components.TvTopBar
import com.mangatv.reader.ui.theme.AccentCyan
import com.mangatv.reader.ui.theme.AccentOrange
import com.mangatv.reader.ui.theme.AccentTeal
import com.mangatv.reader.ui.theme.CinemaCardBg
import com.mangatv.reader.ui.theme.CinemaSurface
import com.mangatv.reader.ui.theme.CinemaSurfaceVariant
import com.mangatv.reader.ui.theme.TextDark
import com.mangatv.reader.ui.theme.TextMuted
import com.mangatv.reader.ui.theme.TextWhite
import java.io.File

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun TvLibraryScreen(
    onNavigateToReader: (filePath: String) -> Unit,
    onNavigateToTab: (TvNavTab) -> Unit,
    viewModel: TvLibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val removeDialogFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.loadLibrary()
    }

    BackHandler(enabled = uiState.confirmRemoveRecent != null) {
        viewModel.dismissRemovePrompt()
    }

    LaunchedEffect(uiState.confirmRemoveRecent) {
        if (uiState.confirmRemoveRecent != null) {
            try {
                removeDialogFocusRequester.requestFocus()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    TvSafeAreaBox {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Top Navigation
            TvTopBar(
                selectedTab = TvNavTab.LIBRARY,
                onTabSelected = onNavigateToTab
            )

            // Main Content Area
            if (uiState.allComics.isEmpty() && !uiState.isLoading) {
                // Empty Library State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.height(64.dp).width(64.dp)
                        )
                        Text(
                            text = "No Manga or Comics Discovered Yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Go to Storage & Shares to browse folders or NAS network drives.",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                        )
                        Button(
                            onClick = { onNavigateToTab(TvNavTab.EXPLORER) },
                            colors = ButtonDefaults.colors(
                                containerColor = AccentCyan,
                                focusedContainerColor = AccentTeal
                            )
                        ) {
                            Text("Open Storage & Shares", color = TextDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // "Continue Reading" Shelf Header (if recents exist)
                    if (uiState.recentComics.isNotEmpty()) {
                        item(span = { GridItemSpan(5) }) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text(
                                    text = "Continue Reading",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = AccentOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                                ) {
                                    items(uiState.recentComics, key = { it.path }) { recent ->
                                        RecentComicShelfCard(
                                            recent = recent,
                                            onClick = {
                                                viewModel.setLastFocusedPath(recent.path)
                                                onNavigateToReader(recent.path)
                                            },
                                            onLongClick = {
                                                viewModel.promptRemoveFromContinueReading(recent)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "All Comics (${uiState.allComics.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Grid Comic Posters
                    items(uiState.allComics, key = { it.path }) { comic ->
                        ComicPosterCard(
                            comic = comic,
                            onClick = {
                                viewModel.setLastFocusedPath(comic.path)
                                onNavigateToReader(comic.path)
                            },
                            onLongClick = {
                                viewModel.openMetadataDrawer(comic)
                            }
                        )
                    }
                }
            }
        }

        // Side Metadata Drawer Overlay
        AnimatedVisibility(
            visible = uiState.isDrawerOpen,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            uiState.selectedComicForDrawer?.let { item ->
                ComicMetadataDrawer(
                    comic = item,
                    onClose = { viewModel.closeMetadataDrawer() },
                    onReadNow = {
                        viewModel.closeMetadataDrawer()
                        onNavigateToReader(item.path)
                    }
                )
            }
        }

        // Remove from Continue Reading Confirmation Dialog
        if (uiState.confirmRemoveRecent != null) {
            val recentItem = uiState.confirmRemoveRecent!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .width(440.dp)
                        .background(CinemaSurface, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Remove from Continue Reading?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "\"${recentItem.title}\" will be removed from Continue Reading. Your comic file remains in the library.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.confirmRemoveFromContinueReading() },
                            modifier = Modifier.focusRequester(removeDialogFocusRequester),
                            colors = ButtonDefaults.colors(
                                containerColor = AccentOrange,
                                focusedContainerColor = AccentCyan
                            )
                        ) {
                            Text("Remove", color = TextDark, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.dismissRemovePrompt() },
                            colors = ButtonDefaults.colors(
                                containerColor = CinemaSurfaceVariant,
                                focusedContainerColor = AccentCyan
                            )
                        ) {
                            Text("Cancel", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicPosterCard(
    comic: ComicItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    TvFocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth()
    ) { isFocused ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f) // Standard 1:1.43 comic cover ratio
                    .background(CinemaSurfaceVariant)
            ) {
                if (comic.coverPath != null) {
                    AsyncImage(
                        model = File(comic.coverPath),
                        contentDescription = comic.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.height(48.dp).width(48.dp)
                        )
                    }
                }

                // Progress Badge
                TvReadingBadge(
                    currentPage = comic.currentPage,
                    totalPages = comic.totalPages,
                    isCompleted = comic.isCompleted,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                )

                // Archive format pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (comic.totalPages > 0) 10.dp else 6.dp, start = 6.dp)
                        .background(Color(0xCC0D1117), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = comic.extension.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, color = AccentCyan)
                    )
                }

                // Reading Progress Percentage Bar at bottom of poster
                if (comic.totalPages > 0) {
                    TvProgressBar(
                        currentPage = comic.currentPage,
                        totalPages = comic.totalPages,
                        isCompleted = comic.isCompleted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            // Title & Chapter Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = comic.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                        color = if (isFocused) AccentCyan else TextWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (comic.totalPages > 0) "${comic.totalPages} pages" else "${comic.fileSize / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = TextMuted)
                )
            }
        }
    }
}

@Composable
private fun RecentComicShelfCard(
    recent: ComicProgressEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    TvFocusableCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.width(220.dp).height(120.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CinemaSurfaceVariant)
            ) {
                if (recent.coverPath != null) {
                    AsyncImage(
                        model = File(recent.coverPath),
                        contentDescription = recent.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recent.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFocused) AccentCyan else TextWhite
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (recent.totalPages > 0) {
                        TvProgressBar(
                            currentPage = recent.currentPage,
                            totalPages = recent.totalPages,
                            isCompleted = recent.isCompleted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }

                    TvReadingBadge(
                        currentPage = recent.currentPage,
                        totalPages = recent.totalPages,
                        isCompleted = recent.isCompleted
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicMetadataDrawer(
    comic: ComicItem,
    onClose: () -> Unit,
    onReadNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(420.dp)
            .background(CinemaSurface, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comic Details",
                    style = MaterialTheme.typography.titleLarge.copy(color = AccentCyan)
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.colors(
                        containerColor = CinemaSurfaceVariant,
                        focusedContainerColor = AccentOrange
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                }
            }

            Text(
                text = comic.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            comic.metadata?.summary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaCardBg, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                comic.metadata?.series?.let { MetadataRow("Series", it) }
                comic.metadata?.number?.let { MetadataRow("Volume/Issue", it) }
                comic.metadata?.writer?.let { MetadataRow("Writer", it) }
                comic.metadata?.penciller?.let { MetadataRow("Artist", it) }
                MetadataRow("Format", comic.extension.uppercase())
                MetadataRow("Location", comic.parentDirectory)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onReadNow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = AccentCyan,
                    focusedContainerColor = AccentTeal
                )
            ) {
                Text(
                    text = if (comic.currentPage > 0) "Resume Reading (p. ${comic.currentPage})" else "Start Reading",
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 13.sp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite, fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
