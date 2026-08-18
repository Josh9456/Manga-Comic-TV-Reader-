package com.mangatv.reader.ui.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangatv.reader.data.repository.ComicRepository
import com.mangatv.reader.domain.archive.DecoderFactory
import com.mangatv.reader.domain.engine.MarginCropEngine
import com.mangatv.reader.domain.engine.PageCacheEngine
import com.mangatv.reader.domain.model.AspectRatioMode
import com.mangatv.reader.domain.model.ComicInfoMetadata
import com.mangatv.reader.domain.model.ReadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class NavDirection {
    FORWARD,
    BACKWARD,
    NONE
}

data class ReaderUiState(
    val filePath: String = "",
    val title: String = "",
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val currentBitmap: Bitmap? = null,
    val secondaryBitmap: Bitmap? = null,
    val isCurrentSpreadDual: Boolean = false,
    val spreadMode: com.mangatv.reader.domain.model.PageSpreadMode = com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE,
    val isOsdVisible: Boolean = false,
    val readingMode: ReadingMode = ReadingMode.RTL,
    val aspectMode: AspectRatioMode = AspectRatioMode.FIT_SCREEN,
    val isAutoCropEnabled: Boolean = false,
    val isSlideshowActive: Boolean = false,
    val slideshowIntervalSeconds: Int = 8,
    val isOledDimmed: Boolean = false,
    val isAtEndPromptVisible: Boolean = false,
    val nextVolumePath: String? = null,
    val comicInfo: ComicInfoMetadata? = null,
    val panOffsetY: Float = 0f,
    val panOffsetX: Float = 0f,
    val zoomScale: Float = 1.0f,
    val navDirection: NavDirection = NavDirection.NONE,
    val isLoading: Boolean = true
)

class TvComicReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val comicRepository = ComicRepository(application)
    private var cacheEngine: PageCacheEngine? = null
    private var slideshowJob: Job? = null
    private var idleTimerJob: Job? = null

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun openComic(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(filePath = filePath, isLoading = true)

            val file = File(filePath)
            if (!file.exists()) return@launch

            val decoder = DecoderFactory.createDecoder(file) ?: return@launch
            val engine = PageCacheEngine(decoder, viewModelScope)
            cacheEngine = engine

            val total = engine.totalPages
            val comicInfo = decoder.getComicInfo()

            // Restore saved progress if available
            val savedProgress = comicRepository.getProgressForPath(filePath)
            val initialIndex = if (savedProgress != null && savedProgress.currentPage > 0) {
                (savedProgress.currentPage - 1).coerceIn(0, (total - 1).coerceAtLeast(0))
            } else {
                0
            }

            // Determine initial reading mode from ComicInfo or saved progress
            val initialMode = if (savedProgress != null) {
                ReadingMode.fromString(savedProgress.readingMode)
            } else if (comicInfo?.manga == true) {
                ReadingMode.RTL
            } else if (comicInfo?.manga == false) {
                ReadingMode.LTR
            } else {
                ReadingMode.RTL
            }

            val initialAspect = if (savedProgress != null) {
                AspectRatioMode.fromString(savedProgress.aspectMode)
            } else {
                AspectRatioMode.FIT_SCREEN
            }

            val initialSpread = if (savedProgress != null) {
                com.mangatv.reader.domain.model.PageSpreadMode.fromString(savedProgress.spreadMode)
            } else {
                com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE
            }

            // Find next volume in directory
            val parent = file.parentFile
            var nextFile: String? = null
            if (parent != null) {
                val siblings = (parent.listFiles() ?: emptyArray())
                    .filter { DecoderFactory.isSupportedFile(it) }
                    .sortedBy { it.name.lowercase() }
                val currentIndex = siblings.indexOfFirst { it.absolutePath == file.absolutePath }
                if (currentIndex in 0 until siblings.size - 1) {
                    nextFile = siblings[currentIndex + 1].absolutePath
                }
            }

            _uiState.value = _uiState.value.copy(
                title = comicInfo?.title ?: file.nameWithoutExtension,
                currentPageIndex = initialIndex,
                totalPages = total,
                readingMode = initialMode,
                aspectMode = initialAspect,
                spreadMode = initialSpread,
                comicInfo = comicInfo,
                nextVolumePath = nextFile,
                isLoading = false
            )

            // Listen for cache updates
            launch {
                engine.pageUpdateFlow.collect { updatedIndex ->
                    val cur = _uiState.value.currentPageIndex
                    if (updatedIndex == cur || updatedIndex == cur + 1) {
                        loadCurrentBitmap(cur)
                    }
                }
            }

            engine.updateCurrentPosition(initialIndex)
            loadCurrentBitmap(initialIndex)
            resetIdleTimer()
        }
    }

    private fun loadCurrentBitmap(index: Int) {
        val engine = cacheEngine ?: return
        var bitmap1 = engine.getPageBitmap(index)
        if (bitmap1 != null && _uiState.value.isAutoCropEnabled) {
            bitmap1 = MarginCropEngine.autoCropMargins(bitmap1)
        }

        val spreadMode = _uiState.value.spreadMode
        val total = _uiState.value.totalPages

        if (spreadMode == com.mangatv.reader.domain.model.PageSpreadMode.SINGLE_PAGE) {
            _uiState.value = _uiState.value.copy(
                currentBitmap = bitmap1,
                secondaryBitmap = null,
                isCurrentSpreadDual = false,
                panOffsetY = 0f,
                panOffsetX = 0f
            )
            return
        }

        // DUAL PAGE SPREAD MODE:
        // Page 0 (Cover page) -> Display single cover
        if (index == 0) {
            _uiState.value = _uiState.value.copy(
                currentBitmap = bitmap1,
                secondaryBitmap = null,
                isCurrentSpreadDual = false,
                panOffsetY = 0f,
                panOffsetX = 0f
            )
            return
        }

        // If primary page is already a landscape spread (width > height), display solo
        if (bitmap1 != null && bitmap1.width > bitmap1.height) {
            _uiState.value = _uiState.value.copy(
                currentBitmap = bitmap1,
                secondaryBitmap = null,
                isCurrentSpreadDual = false,
                panOffsetY = 0f,
                panOffsetX = 0f
            )
            return
        }

        // Pair with next page if available and next page is not landscape
        if (index + 1 < total) {
            var bitmap2 = engine.getPageBitmap(index + 1)
            if (bitmap2 != null && bitmap2.width > bitmap2.height) {
                // Next page is a landscape spread, so current portrait page stays solo
                _uiState.value = _uiState.value.copy(
                    currentBitmap = bitmap1,
                    secondaryBitmap = null,
                    isCurrentSpreadDual = false,
                    panOffsetY = 0f,
                    panOffsetX = 0f
                )
            } else {
                if (bitmap2 != null && _uiState.value.isAutoCropEnabled) {
                    bitmap2 = MarginCropEngine.autoCropMargins(bitmap2)
                }
                _uiState.value = _uiState.value.copy(
                    currentBitmap = bitmap1,
                    secondaryBitmap = bitmap2,
                    isCurrentSpreadDual = true,
                    panOffsetY = 0f,
                    panOffsetX = 0f
                )
            }
        } else {
            // Last page solo
            _uiState.value = _uiState.value.copy(
                currentBitmap = bitmap1,
                secondaryBitmap = null,
                isCurrentSpreadDual = false,
                panOffsetY = 0f,
                panOffsetX = 0f
            )
        }
    }

    fun nextPage() {
        resetIdleTimer()
        val current = _uiState.value.currentPageIndex
        val total = _uiState.value.totalPages
        val isDual = _uiState.value.isCurrentSpreadDual

        val step = if (isDual) 2 else 1
        val nextIndex = current + step

        if (nextIndex < total) {
            _uiState.value = _uiState.value.copy(
                currentPageIndex = nextIndex,
                isAtEndPromptVisible = false,
                navDirection = NavDirection.FORWARD,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
            cacheEngine?.updateCurrentPosition(nextIndex, readingForward = true)
            loadCurrentBitmap(nextIndex)
            saveCurrentProgress()
        } else if (current < total - 1) {
            // Final single page
            val finalIndex = total - 1
            _uiState.value = _uiState.value.copy(
                currentPageIndex = finalIndex,
                isAtEndPromptVisible = false,
                navDirection = NavDirection.FORWARD,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
            cacheEngine?.updateCurrentPosition(finalIndex, readingForward = true)
            loadCurrentBitmap(finalIndex)
            saveCurrentProgress()
        } else {
            // Reached last page
            _uiState.value = _uiState.value.copy(isAtEndPromptVisible = true)
        }
    }

    fun prevPage() {
        resetIdleTimer()
        val current = _uiState.value.currentPageIndex
        if (current <= 0) return

        val isDualMode = _uiState.value.spreadMode == com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE

        val prevIndex = if (isDualMode) {
            if (current <= 2) {
                // If stepping back from pages 1 or 2, go back to cover (page 0)
                0
            } else {
                // Step back by 2 pages, aligning to the start of the previous spread
                val candidate = current - 2
                if (candidate % 2 == 0 && candidate > 0) candidate - 1 else candidate
            }
        } else {
            current - 1
        }.coerceAtLeast(0)

        _uiState.value = _uiState.value.copy(
            currentPageIndex = prevIndex,
            isAtEndPromptVisible = false,
            navDirection = NavDirection.BACKWARD,
            panOffsetX = 0f,
            panOffsetY = 0f
        )
        cacheEngine?.updateCurrentPosition(prevIndex, readingForward = false)
        loadCurrentBitmap(prevIndex)
        saveCurrentProgress()
    }

    fun jumpToPage(targetPage: Int) {
        resetIdleTimer()
        val current = _uiState.value.currentPageIndex
        val total = _uiState.value.totalPages
        val target = targetPage.coerceIn(0, total - 1)
        val dir = if (target > current) NavDirection.FORWARD else if (target < current) NavDirection.BACKWARD else NavDirection.NONE
        _uiState.value = _uiState.value.copy(
            currentPageIndex = target,
            isAtEndPromptVisible = false,
            navDirection = dir,
            panOffsetX = 0f,
            panOffsetY = 0f
        )
        cacheEngine?.updateCurrentPosition(target)
        loadCurrentBitmap(target)
        saveCurrentProgress()
    }

    fun jumpPages(delta: Int) {
        jumpToPage(_uiState.value.currentPageIndex + delta)
    }

    fun panVertical(deltaY: Float, maxPanY: Float = Float.MAX_VALUE) {
        resetIdleTimer()
        val currentY = _uiState.value.panOffsetY
        val newY = (currentY + deltaY).coerceIn(-maxPanY, maxPanY)
        _uiState.value = _uiState.value.copy(panOffsetY = newY)
    }

    fun panHorizontal(deltaX: Float, maxPanX: Float = Float.MAX_VALUE) {
        resetIdleTimer()
        val currentX = _uiState.value.panOffsetX
        val newX = (currentX + deltaX).coerceIn(-maxPanX, maxPanX)
        _uiState.value = _uiState.value.copy(panOffsetX = newX)
    }

    fun panBy(deltaX: Float, deltaY: Float, maxPanX: Float, maxPanY: Float) {
        resetIdleTimer()
        val currentX = _uiState.value.panOffsetX
        val currentY = _uiState.value.panOffsetY
        val newX = if (maxPanX > 0f) (currentX + deltaX).coerceIn(-maxPanX, maxPanX) else 0f
        val newY = if (maxPanY > 0f) (currentY + deltaY).coerceIn(-maxPanY, maxPanY) else 0f
        _uiState.value = _uiState.value.copy(panOffsetX = newX, panOffsetY = newY)
    }

    fun setZoomScale(scale: Float) {
        resetIdleTimer()
        val clamped = scale.coerceIn(1.0f, 3.0f)
        if (clamped == 1.0f) {
            _uiState.value = _uiState.value.copy(zoomScale = clamped, panOffsetX = 0f, panOffsetY = 0f)
        } else {
            _uiState.value = _uiState.value.copy(zoomScale = clamped)
        }
    }

    fun zoomIn() {
        val current = _uiState.value.zoomScale
        val next = when {
            current < 1.25f -> 1.25f
            current < 1.5f -> 1.5f
            current < 2.0f -> 2.0f
            current < 3.0f -> 3.0f
            else -> 3.0f
        }
        setZoomScale(next)
    }

    fun zoomOut() {
        val current = _uiState.value.zoomScale
        val next = when {
            current > 2.0f -> 2.0f
            current > 1.5f -> 1.5f
            current > 1.25f -> 1.25f
            else -> 1.0f
        }
        setZoomScale(next)
    }

    fun resetZoomAndPan() {
        _uiState.value = _uiState.value.copy(zoomScale = 1.0f, panOffsetX = 0f, panOffsetY = 0f)
    }

    fun toggleOsd() {
        resetIdleTimer()
        _uiState.value = _uiState.value.copy(isOsdVisible = !_uiState.value.isOsdVisible)
    }

    fun hideOsd() {
        _uiState.value = _uiState.value.copy(isOsdVisible = false)
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.value = _uiState.value.copy(readingMode = mode)
        saveCurrentProgress()
    }

    fun setAspectRatio(aspect: AspectRatioMode) {
        _uiState.value = _uiState.value.copy(aspectMode = aspect, panOffsetY = 0f, panOffsetX = 0f, zoomScale = 1.0f)
        saveCurrentProgress()
    }

    fun toggleAutoCrop() {
        val newCrop = !_uiState.value.isAutoCropEnabled
        _uiState.value = _uiState.value.copy(isAutoCropEnabled = newCrop)
        loadCurrentBitmap(_uiState.value.currentPageIndex)
    }

    fun toggleSlideshow() {
        if (_uiState.value.isSlideshowActive) {
            slideshowJob?.cancel()
            _uiState.value = _uiState.value.copy(isSlideshowActive = false)
        } else {
            _uiState.value = _uiState.value.copy(isSlideshowActive = true)
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        slideshowJob?.cancel()
        slideshowJob = viewModelScope.launch {
            while (isActive) {
                delay(_uiState.value.slideshowIntervalSeconds * 1000L)
                if (_uiState.value.currentPageIndex < _uiState.value.totalPages - 1) {
                    nextPage()
                } else {
                    _uiState.value = _uiState.value.copy(isSlideshowActive = false)
                    break
                }
            }
        }
    }

    fun resetIdleTimer() {
        if (_uiState.value.isOledDimmed) {
            _uiState.value = _uiState.value.copy(isOledDimmed = false)
        }
        idleTimerJob?.cancel()
        idleTimerJob = viewModelScope.launch {
            delay(180_000L) // 3 minutes idle time
            _uiState.value = _uiState.value.copy(isOledDimmed = true)
        }
    }

    fun togglePageSpreadMode() {
        val current = _uiState.value.spreadMode
        val next = if (current == com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE) {
            com.mangatv.reader.domain.model.PageSpreadMode.SINGLE_PAGE
        } else {
            com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE
        }
        _uiState.value = _uiState.value.copy(spreadMode = next)
        loadCurrentBitmap(_uiState.value.currentPageIndex)
        saveCurrentProgress()
    }

    fun saveCurrentProgress() {
        val state = _uiState.value
        if (state.filePath.isEmpty() || state.totalPages == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            comicRepository.saveProgress(
                path = state.filePath,
                title = state.title,
                currentPage = state.currentPageIndex + 1,
                totalPages = state.totalPages,
                isCompleted = state.currentPageIndex >= state.totalPages - 1,
                readingMode = state.readingMode,
                aspectMode = state.aspectMode,
                spreadMode = state.spreadMode
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgress()
        slideshowJob?.cancel()
        idleTimerJob?.cancel()
        cacheEngine?.release()
    }
}
