package com.mangatv.reader.data.repository

import android.content.Context
import com.mangatv.reader.data.db.AppDatabase
import com.mangatv.reader.data.db.entity.ComicProgressEntity
import com.mangatv.reader.domain.archive.DecoderFactory
import com.mangatv.reader.domain.model.AspectRatioMode
import com.mangatv.reader.domain.model.ComicItem
import com.mangatv.reader.domain.model.ReadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class ComicRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val comicDao = db.comicDao()
    private val coversDir = File(context.cacheDir, "covers").apply { mkdirs() }

    fun getAllHistory(): Flow<List<ComicProgressEntity>> = comicDao.getAllHistory()

    suspend fun getAllHistoryList(): List<ComicProgressEntity> = comicDao.getAllHistoryList()

    fun getCoverFileForPath(path: String): File {
        val hash = hashPath(path)
        return File(coversDir, "$hash.webp")
    }

    suspend fun importComicFile(file: File): ComicItem? = withContext(Dispatchers.IO) {
        if (!file.exists() || !DecoderFactory.isSupportedFile(file)) return@withContext null

        val coverFile = getCoverFileForPath(file.absolutePath)
        var coverPath = if (coverFile.exists()) coverFile.absolutePath else null
        var totalPages = 0
        var comicTitle = file.nameWithoutExtension

        try {
            val decoder = DecoderFactory.createDecoder(file)
            if (decoder != null) {
                totalPages = decoder.getPageCount()
                val comicInfo = decoder.getComicInfo()
                if (comicInfo?.title?.isNotBlank() == true) {
                    comicTitle = comicInfo.title
                }
                if (coverPath == null && decoder.extractCoverThumbnail(coverFile)) {
                    coverPath = coverFile.absolutePath
                }
                decoder.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var progress = comicDao.getProgressForPath(file.absolutePath)
        if (progress == null) {
            val entity = ComicProgressEntity(
                path = file.absolutePath,
                title = comicTitle,
                currentPage = 0,
                totalPages = totalPages,
                isCompleted = false,
                lastReadTimestamp = System.currentTimeMillis(),
                readingMode = ReadingMode.RTL.name,
                aspectMode = AspectRatioMode.FIT_SCREEN.name,
                coverPath = coverPath
            )
            comicDao.insertOrUpdate(entity)
            progress = entity
        } else {
            val updated = progress.copy(
                totalPages = if (progress.totalPages > 0) progress.totalPages else totalPages,
                coverPath = coverPath ?: progress.coverPath,
                title = if (progress.title.isNotBlank()) progress.title else comicTitle
            )
            comicDao.insertOrUpdate(updated)
            progress = updated
        }

        ComicItem(
            path = file.absolutePath,
            name = comicTitle,
            parentDirectory = file.parent ?: "",
            extension = file.extension.lowercase(),
            isDirectory = file.isDirectory,
            fileSize = file.length(),
            lastModified = file.lastModified(),
            coverPath = coverPath,
            currentPage = progress.currentPage,
            totalPages = if (progress.totalPages > 0) progress.totalPages else totalPages,
            isCompleted = progress.isCompleted,
            readingMode = ReadingMode.fromString(progress.readingMode),
            aspectMode = AspectRatioMode.fromString(progress.aspectMode),
            spreadMode = com.mangatv.reader.domain.model.PageSpreadMode.fromString(progress.spreadMode)
        )
    }

    fun getRecentUnfinished(limit: Int = 10): Flow<List<ComicProgressEntity>> =
        comicDao.getRecentUnfinished(limit)

    suspend fun removeFromContinueReading(path: String) = withContext(Dispatchers.IO) {
        val existing = comicDao.getProgressForPath(path)
        if (existing != null) {
            comicDao.insertOrUpdate(existing.copy(currentPage = 0, lastReadTimestamp = 0L))
        }
    }

    suspend fun getProgressForPath(path: String): ComicProgressEntity? =
        comicDao.getProgressForPath(path)

    suspend fun saveProgress(
        path: String,
        title: String,
        currentPage: Int,
        totalPages: Int,
        isCompleted: Boolean,
        readingMode: ReadingMode,
        aspectMode: AspectRatioMode,
        spreadMode: com.mangatv.reader.domain.model.PageSpreadMode = com.mangatv.reader.domain.model.PageSpreadMode.DUAL_PAGE,
        coverPath: String? = null
    ) = withContext(Dispatchers.IO) {
        val existing = comicDao.getProgressForPath(path)
        val entity = ComicProgressEntity(
            path = path,
            title = title,
            series = existing?.series,
            number = existing?.number,
            currentPage = currentPage,
            totalPages = totalPages,
            isCompleted = isCompleted,
            lastReadTimestamp = System.currentTimeMillis(),
            readingMode = readingMode.name,
            aspectMode = aspectMode.name,
            spreadMode = spreadMode.name,
            coverPath = coverPath ?: existing?.coverPath
        )
        comicDao.insertOrUpdate(entity)
    }

    suspend fun scanDirectory(directory: File): List<ComicItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val files = directory.listFiles() ?: return@withContext emptyList()
        val comicItems = mutableListOf<ComicItem>()

        for (file in files) {
            if (file.isHidden || file.name.startsWith(".")) continue

            if (DecoderFactory.isSupportedFile(file)) {
                val coverFile = getCoverFileForPath(file.absolutePath)
                var coverPath = if (coverFile.exists()) coverFile.absolutePath else null

                // Extract cover thumbnail if not already present
                if (coverPath == null) {
                    try {
                        val decoder = DecoderFactory.createDecoder(file)
                        if (decoder != null) {
                            if (decoder.extractCoverThumbnail(coverFile)) {
                                coverPath = coverFile.absolutePath
                            }
                            decoder.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val progress = comicDao.getProgressForPath(file.absolutePath)

                comicItems.add(
                    ComicItem(
                        path = file.absolutePath,
                        name = file.nameWithoutExtension,
                        parentDirectory = file.parent ?: "",
                        extension = file.extension.lowercase(),
                        isDirectory = file.isDirectory,
                        fileSize = file.length(),
                        lastModified = file.lastModified(),
                        coverPath = coverPath,
                        currentPage = progress?.currentPage ?: 0,
                        totalPages = progress?.totalPages ?: 0,
                        isCompleted = progress?.isCompleted ?: false,
                        readingMode = ReadingMode.fromString(progress?.readingMode),
                        aspectMode = AspectRatioMode.fromString(progress?.aspectMode),
                        spreadMode = com.mangatv.reader.domain.model.PageSpreadMode.fromString(progress?.spreadMode)
                    )
                )
            }
        }

        comicItems.sortedBy { it.name.lowercase() }
    }

    private fun hashPath(path: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(path.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
