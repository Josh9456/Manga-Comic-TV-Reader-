package com.mangatv.reader.domain.engine

import android.graphics.Bitmap
import android.util.LruCache
import com.mangatv.reader.domain.archive.ComicArchiveDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PageCacheEngine(
    private val decoder: ComicArchiveDecoder,
    private val scope: CoroutineScope,
    private val targetWidth: Int = 1920,
    private val targetHeight: Int = 1080
) {
    // 8 to 10 decoded pages in memory for dual-page spreads (~60MB)
    private val maxMemoryCacheSize = 10

    private val bitmapCache = object : LruCache<Int, Bitmap>(maxMemoryCacheSize) {
        override fun sizeOf(key: Int, value: Bitmap): Int = 1
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap?, newValue: Bitmap?) {
            // Note: Let GC handle bitmap recycling to avoid recycling in-use Compose images
        }
    }

    private val _pageUpdateFlow = MutableStateFlow<Int>(-1)
    val pageUpdateFlow: StateFlow<Int> = _pageUpdateFlow.asStateFlow()

    private var prefetchJob: Job? = null

    val totalPages: Int
        get() = decoder.getPageCount()

    fun getPageBitmap(index: Int): Bitmap? {
        synchronized(bitmapCache) {
            val cached = bitmapCache.get(index)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
        }
        // Trigger on-demand load if missing
        requestPageLoad(index)
        return null
    }

    fun requestPageLoad(index: Int) {
        if (index < 0 || index >= totalPages) return
        scope.launch(Dispatchers.IO) {
            decodeAndCache(index)
        }
    }

    fun updateCurrentPosition(currentIndex: Int, readingForward: Boolean = true) {
        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.IO) {
            val targets = if (readingForward) {
                listOf(currentIndex, currentIndex + 1, currentIndex + 2, currentIndex + 3, currentIndex - 1, currentIndex - 2)
            } else {
                listOf(currentIndex, currentIndex - 1, currentIndex - 2, currentIndex - 3, currentIndex + 1, currentIndex + 2)
            }

            for (target in targets) {
                if (target in 0 until totalPages) {
                    var needsLoad = false
                    synchronized(bitmapCache) {
                        if (bitmapCache.get(target) == null) {
                            needsLoad = true
                        }
                    }
                    if (needsLoad) {
                        decodeAndCache(target)
                    }
                }
            }
        }
    }

    private suspend fun decodeAndCache(index: Int) {
        if (index !in 0 until totalPages) return
        val bitmap = withContext(Dispatchers.IO) {
            try {
                decoder.getPageBitmap(index, targetWidth, targetHeight)
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            synchronized(bitmapCache) {
                bitmapCache.put(index, bitmap)
            }
            _pageUpdateFlow.emit(index)
        }
    }

    fun clearCache() {
        prefetchJob?.cancel()
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
    }

    fun release() {
        clearCache()
        decoder.close()
    }
}
