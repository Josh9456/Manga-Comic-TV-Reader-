package com.mangatv.reader.ui.explorer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangatv.reader.data.db.entity.BookmarkedDirectoryEntity
import com.mangatv.reader.data.db.entity.SmbShareEntity
import com.mangatv.reader.data.network.SmbFileItem
import com.mangatv.reader.data.network.SmbStorageManager
import com.mangatv.reader.data.repository.ComicRepository
import com.mangatv.reader.data.repository.StorageDrive
import com.mangatv.reader.data.repository.StorageRepository
import com.mangatv.reader.domain.archive.DecoderFactory
import com.mangatv.reader.domain.archive.ImageDecoderUtils
import com.mangatv.reader.domain.model.ComicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ExplorerUiState(
    val drives: List<StorageDrive> = emptyList(),
    val bookmarks: List<BookmarkedDirectoryEntity> = emptyList(),
    val smbShares: List<SmbShareEntity> = emptyList(),
    val currentPath: String = "",
    val currentDirectoryName: String = "Storage",
    val directoryFiles: List<File> = emptyList(),
    val isCurrentBookmarked: Boolean = false,
    val isLoading: Boolean = false,
    val isSelectMode: Boolean = false,
    val selectedPaths: Set<String> = emptySet(),
    val isImporting: Boolean = false,
    val importProgressMessage: String? = null,
    val isSmbActive: Boolean = false,
    val activeSmbShare: SmbShareEntity? = null,
    val smbFiles: List<SmbFileItem> = emptyList(),
    val smbPath: String = "",
    val isSmbDialogOpen: Boolean = false,
    val isConnectingSmb: Boolean = false,
    val smbErrorMessage: String? = null,
    val isDownloadingSmb: Boolean = false,
    val downloadProgressMessage: String? = null,
    val statusMessage: String? = null
)

class TvFileManagerViewModel(private val app: Application) : AndroidViewModel(app) {

    private val storageRepository = StorageRepository(app)
    private val comicRepository = ComicRepository(app)
    val smbManager = SmbStorageManager()

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        loadDrivesAndBookmarks()
    }

    fun loadDrivesAndBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            val drives = storageRepository.getAvailableDrives()
            _uiState.value = _uiState.value.copy(drives = drives)

            if (_uiState.value.currentPath.isEmpty() && drives.isNotEmpty()) {
                navigateToDirectory(drives.first().path)
            }
        }

        viewModelScope.launch {
            storageRepository.getAllBookmarks().collect { bms ->
                _uiState.value = _uiState.value.copy(bookmarks = bms)
                checkBookmarkStatus(_uiState.value.currentPath)
            }
        }

        viewModelScope.launch {
            storageRepository.getAllSmbShares().collect { shares ->
                _uiState.value = _uiState.value.copy(smbShares = shares)
            }
        }
    }

    fun openSmbDialog() {
        _uiState.value = _uiState.value.copy(
            isSmbDialogOpen = true,
            isConnectingSmb = false,
            smbErrorMessage = null
        )
    }

    fun dismissSmbDialog() {
        _uiState.value = _uiState.value.copy(
            isSmbDialogOpen = false,
            isConnectingSmb = false,
            smbErrorMessage = null
        )
    }

    fun linkSmbShare(
        address: String,
        explicitShareName: String = "",
        username: String = "",
        password: String = "",
        domain: String = "",
        displayName: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanAddr = address.trim()
                .removePrefix("smb://")
                .removePrefix("\\\\")
                .replace("\\", "/")

            val host: String
            val shareName: String

            if (cleanAddr.contains("/")) {
                host = cleanAddr.substringBefore("/")
                val pathPart = cleanAddr.substringAfter("/").trim('/')
                shareName = pathPart.ifEmpty { explicitShareName.trim() }
            } else {
                host = cleanAddr
                shareName = explicitShareName.trim()
            }

            if (host.isBlank() || shareName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isConnectingSmb = false,
                    smbErrorMessage = "Please specify server address and share name (e.g. 192.168.1.100/Manga)"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isConnectingSmb = true,
                smbErrorMessage = null
            )

            val success = smbManager.connect(
                host = host,
                shareName = shareName,
                username = username,
                password = password,
                domain = domain
            )

            if (success) {
                val resolvedName = displayName.ifBlank {
                    if (shareName.isNotBlank()) shareName else host
                }
                val entity = SmbShareEntity(
                    displayName = resolvedName,
                    host = host,
                    shareName = shareName,
                    username = username,
                    domain = domain,
                    passwordEncrypted = password
                )
                val newId = storageRepository.addSmbShare(entity)
                val savedShare = entity.copy(id = newId)

                _uiState.value = _uiState.value.copy(
                    isSmbDialogOpen = false,
                    isConnectingSmb = false,
                    smbErrorMessage = null,
                    isSmbActive = true,
                    activeSmbShare = savedShare,
                    smbPath = "",
                    currentDirectoryName = "smb://$host/$shareName",
                    statusMessage = "Linked SMB share: $resolvedName"
                )
                navigateSmb("")
            } else {
                _uiState.value = _uiState.value.copy(
                    isConnectingSmb = false,
                    smbErrorMessage = "Failed to connect to SMB share at $host/$shareName. Check IP, share name, and credentials."
                )
            }
        }
    }

    fun openSmbShare(share: SmbShareEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isSmbActive = true,
                activeSmbShare = share,
                smbPath = "",
                selectedPaths = emptySet(),
                isSelectMode = false,
                currentDirectoryName = share.displayName.ifEmpty { "smb://${share.host}/${share.shareName}" }
            )

            val success = smbManager.connect(
                host = share.host,
                shareName = share.shareName,
                username = share.username,
                password = share.passwordEncrypted,
                domain = share.domain
            )

            if (success) {
                navigateSmb("")
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Could not connect to ${share.displayName} (${share.host})"
                )
            }
        }
    }

    fun removeSmbShare(share: SmbShareEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            storageRepository.removeSmbShare(share.id)
            if (_uiState.value.activeSmbShare?.id == share.id) {
                smbManager.disconnect()
                val firstDrive = _uiState.value.drives.firstOrNull()
                if (firstDrive != null) {
                    navigateToDirectory(firstDrive.path)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSmbActive = false,
                        activeSmbShare = null,
                        smbFiles = emptyList()
                    )
                }
            }
        }
    }

    fun downloadAndOpenSmbComic(smbFile: SmbFileItem, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isDownloadingSmb = true,
                downloadProgressMessage = "Opening remote comic: ${smbFile.name}..."
            )

            val cacheDir = File(app.cacheDir, "smb_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val localDest = File(cacheDir, smbFile.name)

            // If file already exists in cache with matching size, reuse it
            if (localDest.exists() && localDest.length() == smbFile.size && smbFile.size > 0) {
                _uiState.value = _uiState.value.copy(isDownloadingSmb = false, downloadProgressMessage = null)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onReady(localDest.absolutePath)
                }
                return@launch
            }

            val success = smbManager.downloadToCache(smbFile.path, localDest)
            _uiState.value = _uiState.value.copy(
                isDownloadingSmb = false,
                downloadProgressMessage = null
            )

            if (success && localDest.exists()) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onReady(localDest.absolutePath)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Failed to download remote file: ${smbFile.name}"
                )
            }
        }
    }

    fun toggleSelectMode() {
        val newMode = !_uiState.value.isSelectMode
        _uiState.value = _uiState.value.copy(
            isSelectMode = newMode,
            selectedPaths = if (!newMode) emptySet() else _uiState.value.selectedPaths
        )
    }

    fun toggleFileSelection(path: String) {
        val current = _uiState.value.selectedPaths.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _uiState.value = _uiState.value.copy(
            selectedPaths = current,
            isSelectMode = if (current.isNotEmpty()) true else _uiState.value.isSelectMode
        )
    }

    fun selectAllComicFiles() {
        val comicFiles = if (_uiState.value.isSmbActive) {
            _uiState.value.smbFiles
                .filter { !it.isDirectory && DecoderFactory.isSupportedExtension(File(it.name).extension) }
                .map { it.path }
                .toSet()
        } else {
            _uiState.value.directoryFiles
                .filter { DecoderFactory.isSupportedFile(it) }
                .map { it.absolutePath }
                .toSet()
        }
        _uiState.value = _uiState.value.copy(
            selectedPaths = comicFiles,
            isSelectMode = comicFiles.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPaths = emptySet(),
            isSelectMode = false
        )
    }

    fun batchImportSelected(onComplete: () -> Unit) {
        val paths = _uiState.value.selectedPaths.toList()
        if (paths.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                importProgressMessage = "Importing ${paths.size} items..."
            )

            var count = 0
            if (_uiState.value.isSmbActive) {
                // Import from SMB share
                val smbCacheDir = File(app.cacheDir, "smb_cache")
                if (!smbCacheDir.exists()) smbCacheDir.mkdirs()

                for ((index, smbPath) in paths.withIndex()) {
                    val smbFile = _uiState.value.smbFiles.find { it.path == smbPath } ?: continue
                    _uiState.value = _uiState.value.copy(
                        importProgressMessage = "Downloading (${index + 1}/${paths.size}): ${smbFile.name}"
                    )
                    val localFile = File(smbCacheDir, smbFile.name)
                    if (!localFile.exists() || localFile.length() != smbFile.size) {
                        smbManager.downloadToCache(smbFile.path, localFile)
                    }
                    if (localFile.exists()) {
                        val item = comicRepository.importComicFile(localFile)
                        if (item != null) count++
                    }
                }
            } else {
                for ((index, path) in paths.withIndex()) {
                    _uiState.value = _uiState.value.copy(
                        importProgressMessage = "Importing (${index + 1}/${paths.size}): ${File(path).name}"
                    )
                    val file = File(path)
                    if (file.exists()) {
                        if (file.isDirectory && !DecoderFactory.isSupportedFile(file)) {
                            storageRepository.addBookmark(file.absolutePath, file.name.ifEmpty { file.absolutePath }, "LOCAL")
                            val comics = comicRepository.scanDirectory(file)
                            count += comics.size
                        } else {
                            val item = comicRepository.importComicFile(file)
                            if (item != null) count++
                            file.parentFile?.let { p ->
                                storageRepository.addBookmark(p.absolutePath, p.name.ifEmpty { p.absolutePath }, "LOCAL")
                            }
                        }
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                isImporting = false,
                isSelectMode = false,
                selectedPaths = emptySet(),
                statusMessage = "Successfully imported $count manga entries"
            )

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun navigateToDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isSmbActive = false,
                activeSmbShare = null,
                selectedPaths = emptySet(),
                isSelectMode = false
            )
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                val files = (dir.listFiles() ?: emptyArray())
                    .filter { !it.isHidden && !it.name.startsWith(".") }
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

                _uiState.value = _uiState.value.copy(
                    currentPath = dir.absolutePath,
                    currentDirectoryName = dir.name.ifEmpty { dir.absolutePath },
                    directoryFiles = files,
                    isLoading = false
                )
                checkBookmarkStatus(dir.absolutePath)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun navigateUp() {
        if (_uiState.value.isSmbActive) {
            val smbPath = _uiState.value.smbPath
            if (smbPath.contains("/")) {
                navigateSmb(smbPath.substringBeforeLast("/"))
            } else {
                navigateSmb("")
            }
            return
        }

        val current = File(_uiState.value.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            navigateToDirectory(parent.absolutePath)
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch(Dispatchers.IO) {
            val path = _uiState.value.currentPath
            if (path.isEmpty()) return@launch

            if (_uiState.value.isCurrentBookmarked) {
                storageRepository.removeBookmark(path)
            } else {
                val dir = File(path)
                storageRepository.addBookmark(path, dir.name.ifEmpty { path }, "LOCAL")
            }
            checkBookmarkStatus(path)
        }
    }

    private suspend fun checkBookmarkStatus(path: String) {
        val isBm = storageRepository.isBookmarked(path)
        _uiState.value = _uiState.value.copy(isCurrentBookmarked = isBm)
    }

    fun navigateSmb(remotePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val files = smbManager.listFiles(remotePath)
            val share = _uiState.value.activeSmbShare
            val baseName = share?.displayName ?: "SMB"
            val dirTitle = if (remotePath.isBlank()) baseName else "$baseName / ${remotePath.replace("/", " / ")}"

            _uiState.value = _uiState.value.copy(
                isSmbActive = true,
                smbPath = remotePath,
                currentDirectoryName = dirTitle,
                smbFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })),
                isLoading = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        smbManager.disconnect()
    }
}
