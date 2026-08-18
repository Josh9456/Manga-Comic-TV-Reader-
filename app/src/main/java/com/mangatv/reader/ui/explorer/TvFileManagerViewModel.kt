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
    val smbFiles: List<SmbFileItem> = emptyList(),
    val smbPath: String = "",
    val statusMessage: String? = null
)

class TvFileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepository = StorageRepository(application)
    private val comicRepository = ComicRepository(application)
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
        val comicFiles = _uiState.value.directoryFiles
            .filter { DecoderFactory.isSupportedFile(it) }
            .map { it.absolutePath }
            .toSet()
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

    fun connectSmb(host: String, shareName: String, user: String = "", pass: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = smbManager.connect(host, shareName, user, pass)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isSmbActive = true,
                    smbPath = "",
                    currentDirectoryName = "\\\\$host\\$shareName",
                    statusMessage = "Connected to SMB share: $shareName"
                )
                navigateSmb("")
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to connect to SMB share at $host"
                )
            }
        }
    }

    fun navigateSmb(remotePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val files = smbManager.listFiles(remotePath)
            _uiState.value = _uiState.value.copy(
                isSmbActive = true,
                smbPath = remotePath,
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
