package com.mangatv.reader.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.mangatv.reader.data.db.entity.SmbShareEntity
import com.mangatv.reader.data.network.SmbFileItem
import com.mangatv.reader.domain.archive.DecoderFactory
import com.mangatv.reader.ui.components.TvFocusableCard
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
import java.io.File

@Composable
fun TvFileManagerScreen(
    onNavigateToReader: (filePath: String) -> Unit,
    onNavigateToTab: (TvNavTab) -> Unit,
    viewModel: TvFileManagerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var smbShareToDelete by remember { mutableStateOf<SmbShareEntity?>(null) }

    TvSafeAreaBox {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TvTopBar(
                    selectedTab = TvNavTab.EXPLORER,
                    onTabSelected = onNavigateToTab
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Sidebar: Drives, SMB Shares & Pinned Bookmarks
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(CinemaSurface, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Storage Locations",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.drives) { drive ->
                                SidebarItem(
                                    title = drive.name,
                                    icon = if (drive.isUsb) Icons.Default.Usb else Icons.Default.Storage,
                                    isSelected = uiState.currentPath == drive.path && !uiState.isSmbActive,
                                    onClick = { viewModel.navigateToDirectory(drive.path) }
                                )
                            }

                            // Persistent SMB Network Shares
                            if (uiState.smbShares.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "SMB Shares",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentTeal
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                items(uiState.smbShares) { share ->
                                    SidebarItem(
                                        title = share.displayName,
                                        icon = Icons.Default.Lan,
                                        isSelected = uiState.isSmbActive && uiState.activeSmbShare?.id == share.id,
                                        onClick = { viewModel.openSmbShare(share) },
                                        onLongClick = { smbShareToDelete = share }
                                    )
                                }
                            }

                            // Pinned Folders
                            if (uiState.bookmarks.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Pinned Folders",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentOrange
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                items(uiState.bookmarks) { bm ->
                                    SidebarItem(
                                        title = bm.displayName,
                                        icon = Icons.Default.Bookmark,
                                        isSelected = uiState.currentPath == bm.path && !uiState.isSmbActive,
                                        onClick = { viewModel.navigateToDirectory(bm.path) }
                                    )
                                }
                            }
                        }
                    }

                    // Right Panel: Directory Explorer & Actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Breadcrumb and action bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CinemaSurface, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Button(
                                    onClick = { viewModel.navigateUp() },
                                    colors = ButtonDefaults.colors(
                                        containerColor = CinemaSurfaceVariant,
                                        focusedContainerColor = AccentCyan
                                    )
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Parent Directory", tint = TextWhite)
                                }

                                Text(
                                    text = if (uiState.isSelectMode) {
                                        "Selected: ${uiState.selectedPaths.size} item(s)"
                                    } else {
                                        uiState.currentDirectoryName
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isSelectMode) AccentCyan else TextWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Action Buttons Toolbar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.isSelectMode) {
                                    // Select All Comic Files
                                    Button(
                                        onClick = { viewModel.selectAllComicFiles() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = CinemaSurfaceVariant,
                                            focusedContainerColor = AccentCyan
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.DoneAll, contentDescription = "Select All", tint = TextWhite, modifier = Modifier.size(18.dp))
                                            Text("Select All", color = TextWhite, style = MaterialTheme.typography.labelLarge, softWrap = false, maxLines = 1)
                                        }
                                    }

                                    // Import Selected (Batch Import Action)
                                    if (uiState.selectedPaths.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                viewModel.batchImportSelected(
                                                    onComplete = {
                                                        onNavigateToTab(TvNavTab.LIBRARY)
                                                    }
                                                )
                                            },
                                            colors = ButtonDefaults.colors(
                                                containerColor = AccentCyan,
                                                focusedContainerColor = AccentTeal
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = "Batch Import", tint = TextDark, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "Import (${uiState.selectedPaths.size})",
                                                    color = TextDark,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    softWrap = false,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    // Cancel / Exit Selection Mode (Horizontal Bubble)
                                    Button(
                                        onClick = { viewModel.clearSelection() },
                                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(20.dp)),
                                        colors = ButtonDefaults.colors(
                                            containerColor = CinemaSurfaceVariant,
                                            focusedContainerColor = AccentCyan
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = TextWhite, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Cancel",
                                                color = TextWhite,
                                                style = MaterialTheme.typography.labelLarge,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    // Toggle Select Mode
                                    Button(
                                        onClick = { viewModel.toggleSelectMode() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = CinemaSurfaceVariant,
                                            focusedContainerColor = AccentCyan
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Checklist, contentDescription = "Multi-Select", tint = TextWhite, modifier = Modifier.size(18.dp))
                                            Text("Multi-Select", color = TextWhite, style = MaterialTheme.typography.labelLarge, softWrap = false, maxLines = 1)
                                        }
                                    }

                                    // Pin / Bookmark Folder Button (Local storage only)
                                    if (!uiState.isSmbActive) {
                                        Button(
                                            onClick = { viewModel.toggleBookmark() },
                                            colors = ButtonDefaults.colors(
                                                containerColor = if (uiState.isCurrentBookmarked) AccentOrange else CinemaSurfaceVariant,
                                                focusedContainerColor = AccentTeal
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (uiState.isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                    contentDescription = "Pin to Library",
                                                    tint = if (uiState.isCurrentBookmarked) TextDark else TextWhite
                                                )
                                                Text(
                                                    text = if (uiState.isCurrentBookmarked) "Pinned" else "Pin Folder",
                                                    color = if (uiState.isCurrentBookmarked) TextDark else TextWhite,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }

                                    // Link SMB Share Button
                                    Button(
                                        onClick = { viewModel.openSmbDialog() },
                                        colors = ButtonDefaults.colors(
                                            containerColor = CinemaSurfaceVariant,
                                            focusedContainerColor = AccentCyan
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lan,
                                                contentDescription = "Link SMB Share",
                                                tint = TextWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "SMB",
                                                color = TextWhite,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bring folders down slightly so top row focus blue highlight ring does not clip under the toolbar
                        Spacer(modifier = Modifier.height(14.dp))

                        // Directory Grid / File Items
                        if (uiState.isSmbActive) {
                            if (uiState.smbFiles.isEmpty() && !uiState.isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "SMB share folder is empty or no accessible items",
                                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.smbFiles, key = { it.path }) { smbFile ->
                                        val isSupportedComic = !smbFile.isDirectory && DecoderFactory.isSupportedExtension(File(smbFile.name).extension)
                                        val isSelected = uiState.selectedPaths.contains(smbFile.path)

                                        SmbFileItemCard(
                                            item = smbFile,
                                            isComicArchive = isSupportedComic,
                                            isSelectMode = uiState.isSelectMode,
                                            isSelected = isSelected,
                                            onToggleSelect = {
                                                viewModel.toggleFileSelection(smbFile.path)
                                            },
                                            onClick = {
                                                if (smbFile.isDirectory) {
                                                    viewModel.navigateSmb(smbFile.path)
                                                } else if (isSupportedComic) {
                                                    viewModel.downloadAndOpenSmbComic(smbFile) { localPath ->
                                                        onNavigateToReader(localPath)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            if (uiState.directoryFiles.isEmpty() && !uiState.isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Directory is empty or no accessible items",
                                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.directoryFiles, key = { it.absolutePath }) { file ->
                                        val isSupportedComic = DecoderFactory.isSupportedFile(file)
                                        val isSelected = uiState.selectedPaths.contains(file.absolutePath)

                                        FileItemCard(
                                            file = file,
                                            isComicArchive = isSupportedComic,
                                            isSelectMode = uiState.isSelectMode,
                                            isSelected = isSelected,
                                            onToggleSelect = {
                                                viewModel.toggleFileSelection(file.absolutePath)
                                            },
                                            onClick = {
                                                if (file.isDirectory && !isSupportedComic) {
                                                    viewModel.navigateToDirectory(file.absolutePath)
                                                } else {
                                                    onNavigateToReader(file.absolutePath)
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

            // Batch Importing Progress Overlay
            if (uiState.isImporting) {
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
                            .background(CinemaSurface, RoundedCornerShape(16.dp))
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = AccentCyan)
                        Text(
                            text = uiState.importProgressMessage ?: "Batch importing manga...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "Extracting covers and indexing into Library...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Remote Comic Download Progress Overlay
            if (uiState.isDownloadingSmb) {
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
                            .background(CinemaSurface, RoundedCornerShape(16.dp))
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = AccentCyan)
                        Text(
                            text = uiState.downloadProgressMessage ?: "Opening remote comic...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "Caching comic from SMB server to local storage...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Link SMB Share Modal Dialog
            LinkSmbDialog(
                isOpen = uiState.isSmbDialogOpen,
                isConnecting = uiState.isConnectingSmb,
                errorMessage = uiState.smbErrorMessage,
                onConnect = { address, shareName, user, pass, displayName ->
                    viewModel.linkSmbShare(
                        address = address,
                        explicitShareName = shareName,
                        username = user,
                        password = pass,
                        displayName = displayName
                    )
                },
                onDismiss = { viewModel.dismissSmbDialog() }
            )

            // Unlink SMB Share Confirmation Dialog
            if (smbShareToDelete != null) {
                val share = smbShareToDelete!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .width(420.dp)
                            .background(CinemaSurface, RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Unlink SMB Share?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Remove \"${share.displayName}\" (${share.host}/${share.shareName}) from your saved shares?",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.removeSmbShare(share)
                                    smbShareToDelete = null
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = AccentOrange,
                                    focusedContainerColor = AccentCyan
                                )
                            ) {
                                Text("Unlink", color = TextDark, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { smbShareToDelete = null },
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
}

@Composable
private fun SidebarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) CinemaSurfaceVariant else Color.Transparent,
            focusedContainerColor = AccentCyan
        ),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isFocused) TextDark else if (isSelected) AccentCyan else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isFocused) TextDark else if (isSelected) TextWhite else TextMuted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmbFileItemCard(
    item: SmbFileItem,
    isComicArchive: Boolean,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    TvFocusableCard(
        onClick = {
            if (isSelectMode) {
                onToggleSelect()
            } else {
                onClick()
            }
        },
        onLongClick = onToggleSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isSelected) AccentCyan.copy(alpha = 0.12f) else Color.Transparent)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected) AccentCyan.copy(alpha = 0.3f)
                        else if (isComicArchive) AccentCyan.copy(alpha = 0.2f)
                        else CinemaSurfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isSelected -> Icons.Default.CheckCircle
                        isComicArchive -> Icons.Default.MenuBook
                        item.isDirectory -> Icons.Default.Folder
                        else -> Icons.Default.DriveFileMove
                    },
                    contentDescription = null,
                    tint = if (isSelected) AccentCyan else if (isComicArchive) AccentCyan else if (item.isDirectory) AccentOrange else TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFocused || isSelected) AccentCyan else TextWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isDirectory) "Remote Folder" else if (item.size > 0) "${item.size / (1024 * 1024)} MB" else "SMB File",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextMuted)
                )
            }

            if (isSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) AccentCyan else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FileItemCard(
    file: File,
    isComicArchive: Boolean,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    TvFocusableCard(
        onClick = {
            if (isSelectMode) {
                onToggleSelect()
            } else {
                onClick()
            }
        },
        onLongClick = onToggleSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) AccentCyan else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isSelected) AccentCyan.copy(alpha = 0.12f) else Color.Transparent)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected) AccentCyan.copy(alpha = 0.3f)
                        else if (isComicArchive) AccentCyan.copy(alpha = 0.2f)
                        else CinemaSurfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isSelected -> Icons.Default.CheckCircle
                        isComicArchive -> Icons.Default.MenuBook
                        file.isDirectory -> Icons.Default.Folder
                        else -> Icons.Default.DriveFileMove
                    },
                    contentDescription = null,
                    tint = if (isSelected) AccentCyan else if (isComicArchive) AccentCyan else if (file.isDirectory) AccentOrange else TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFocused || isSelected) AccentCyan else TextWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.isDirectory) "Folder" else "${file.length() / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextMuted)
                )
            }

            if (isSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) AccentCyan else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LinkSmbDialog(
    isOpen: Boolean,
    isConnecting: Boolean,
    errorMessage: String?,
    onConnect: (address: String, shareName: String, user: String, pass: String, displayName: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var address by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .background(CinemaSurface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lan, contentDescription = null, tint = AccentCyan)
                    Text(
                        text = "Link SMB Share",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 20.sp
                        )
                    )
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = CinemaSurfaceVariant,
                        focusedContainerColor = AccentOrange
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                }
            }

            Text(
                text = "Enter network storage address (e.g. 192.168.1.100/Manga):",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 13.sp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { androidx.compose.material.Text("Server / Share Address *", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = TextWhite,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color(0x44FFFFFF),
                    backgroundColor = CinemaSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    label = { androidx.compose.material.Text("Share Name (opt)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextWhite,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        backgroundColor = CinemaSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { androidx.compose.material.Text("Display Name (opt)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextWhite,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        backgroundColor = CinemaSurfaceVariant
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { androidx.compose.material.Text("Username (opt)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextWhite,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        backgroundColor = CinemaSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { androidx.compose.material.Text("Password (opt)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = TextWhite,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0x44FFFFFF),
                        backgroundColor = CinemaSurfaceVariant
                    )
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFF6B6B), fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = CinemaSurfaceVariant,
                        focusedContainerColor = AccentCyan
                    )
                ) {
                    Text("Cancel", color = TextWhite)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        onConnect(address, shareName, username, password, displayName)
                    },
                    enabled = !isConnecting && address.isNotBlank(),
                    colors = ButtonDefaults.colors(
                        containerColor = AccentCyan,
                        focusedContainerColor = AccentTeal
                    )
                ) {
                    if (isConnecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TextDark,
                                strokeWidth = 2.dp
                            )
                            Text("Connecting...", color = TextDark, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Connect & Link", color = TextDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
