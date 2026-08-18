package com.mangatv.reader.ui.library

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangatv.reader.data.db.entity.ComicProgressEntity
import com.mangatv.reader.data.repository.ComicRepository
import com.mangatv.reader.data.repository.StorageRepository
import com.mangatv.reader.domain.model.ComicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class LibraryUiState(
    val recentComics: List<ComicProgressEntity> = emptyList(),
    val allComics: List<ComicItem> = emptyList(),
    val selectedComicForDrawer: ComicItem? = null,
    val isDrawerOpen: Boolean = false,
    val confirmRemoveRecent: ComicProgressEntity? = null,
    val isLoading: Boolean = true,
    val lastFocusedPath: String? = null
)

class TvLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val comicRepository = ComicRepository(application)
    private val storageRepository = StorageRepository(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
        viewModelScope.launch {
            comicRepository.getAllHistory().collect {
                loadLibrary()
            }
        }
        viewModelScope.launch {
            storageRepository.getAllBookmarks().collect {
                loadLibrary()
            }
        }
        viewModelScope.launch {
            comicRepository.getRecentUnfinished(8).collect { recents ->
                _uiState.value = _uiState.value.copy(recentComics = recents)
            }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val scanDirs = mutableListOf<File>()

            // Add standard storage folders
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads != null && downloads.exists()) scanDirs.add(downloads)

            val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documents != null && documents.exists()) scanDirs.add(documents)

            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (pictures != null && pictures.exists()) scanDirs.add(pictures)

            // Add bookmarked/pinned folders
            val bookmarks = storageRepository.getAllBookmarksList()
            for (bm in bookmarks) {
                val bDir = File(bm.path)
                if (bDir.exists() && bDir.isDirectory && scanDirs.none { it.absolutePath == bDir.absolutePath }) {
                    scanDirs.add(bDir)
                }
            }

            // Scan all discovered comic archives
            val foundComicsMap = mutableMapOf<String, ComicItem>()
            for (dir in scanDirs) {
                for (item in comicRepository.scanDirectory(dir)) {
                    foundComicsMap[item.path] = item
                }
            }

            // Also load all comics registered in database (e.g. batch imported or previously read files)
            val dbComics = comicRepository.getAllHistoryList()
            for (history in dbComics) {
                val file = File(history.path)
                if (file.exists()) {
                    val coverFile = comicRepository.getCoverFileForPath(history.path)
                    val coverPath = if (coverFile.exists()) coverFile.absolutePath else history.coverPath
                    val existing = foundComicsMap[history.path]
                    val totalPages = if (history.totalPages > 0) history.totalPages else (existing?.totalPages ?: 0)

                    foundComicsMap[history.path] = ComicItem(
                        path = history.path,
                        name = history.title.ifEmpty { file.nameWithoutExtension },
                        parentDirectory = file.parent ?: "",
                        extension = file.extension.lowercase(),
                        isDirectory = file.isDirectory,
                        fileSize = file.length(),
                        lastModified = file.lastModified(),
                        coverPath = coverPath,
                        currentPage = history.currentPage,
                        totalPages = totalPages,
                        isCompleted = history.isCompleted,
                        readingMode = com.mangatv.reader.domain.model.ReadingMode.fromString(history.readingMode),
                        aspectMode = com.mangatv.reader.domain.model.AspectRatioMode.fromString(history.aspectMode)
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                allComics = foundComicsMap.values.sortedBy { it.name.lowercase() },
                isLoading = false
            )
        }
    }

    fun openMetadataDrawer(item: ComicItem) {
        _uiState.value = _uiState.value.copy(
            selectedComicForDrawer = item,
            isDrawerOpen = true
        )
    }

    fun closeMetadataDrawer() {
        _uiState.value = _uiState.value.copy(
            isDrawerOpen = false
        )
    }

    fun promptRemoveFromContinueReading(recent: ComicProgressEntity) {
        _uiState.value = _uiState.value.copy(confirmRemoveRecent = recent)
    }

    fun dismissRemovePrompt() {
        _uiState.value = _uiState.value.copy(confirmRemoveRecent = null)
    }

    fun confirmRemoveFromContinueReading() {
        val target = _uiState.value.confirmRemoveRecent ?: return
        viewModelScope.launch {
            comicRepository.removeFromContinueReading(target.path)
            _uiState.value = _uiState.value.copy(confirmRemoveRecent = null)
            loadLibrary()
        }
    }

    fun setLastFocusedPath(path: String) {
        _uiState.value = _uiState.value.copy(lastFocusedPath = path)
    }
}
