package com.mangatv.reader.data.repository

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import com.mangatv.reader.data.db.AppDatabase
import com.mangatv.reader.data.db.entity.BookmarkedDirectoryEntity
import com.mangatv.reader.data.db.entity.SmbShareEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class StorageDrive(
    val name: String,
    val path: String,
    val isUsb: Boolean,
    val totalSpace: Long,
    val freeSpace: Long
)

class StorageRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val bookmarkDao = db.bookmarkDao()
    private val smbShareDao = db.smbShareDao()

    fun getAllBookmarks(): Flow<List<BookmarkedDirectoryEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun getAllBookmarksList(): List<BookmarkedDirectoryEntity> = bookmarkDao.getAllBookmarksList()

    suspend fun addBookmark(path: String, displayName: String, source: String = "LOCAL") {
        bookmarkDao.insertBookmark(
            BookmarkedDirectoryEntity(
                path = path,
                displayName = displayName,
                source = source
            )
        )
    }

    suspend fun removeBookmark(path: String) {
        bookmarkDao.deleteBookmark(path)
    }

    suspend fun isBookmarked(path: String): Boolean = bookmarkDao.isBookmarked(path)

    fun getAllSmbShares(): Flow<List<SmbShareEntity>> = smbShareDao.getAllShares()

    suspend fun addSmbShare(share: SmbShareEntity): Long = smbShareDao.insertShare(share)

    suspend fun removeSmbShare(id: Long) = smbShareDao.deleteShare(id)

    suspend fun getAvailableDrives(): List<StorageDrive> = withContext(Dispatchers.IO) {
        val drives = mutableListOf<StorageDrive>()

        // 1. Primary Internal Storage
        val primaryDir = Environment.getExternalStorageDirectory()
        if (primaryDir != null && primaryDir.exists()) {
            drives.add(
                StorageDrive(
                    name = "Internal Storage",
                    path = primaryDir.absolutePath,
                    isUsb = false,
                    totalSpace = primaryDir.totalSpace,
                    freeSpace = primaryDir.freeSpace
                )
            )
        }

        // 2. Common convenience folders
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null && downloads.exists()) {
            drives.add(
                StorageDrive(
                    name = "Downloads",
                    path = downloads.absolutePath,
                    isUsb = false,
                    totalSpace = downloads.totalSpace,
                    freeSpace = downloads.freeSpace
                )
            )
        }

        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents != null && documents.exists()) {
            drives.add(
                StorageDrive(
                    name = "Documents",
                    path = documents.absolutePath,
                    isUsb = false,
                    totalSpace = documents.totalSpace,
                    freeSpace = documents.freeSpace
                )
            )
        }

        // 3. Scan for USB drives & Secondary SD Cards
        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                // Parse root of external storage mount point
                val path = dir.absolutePath
                val rootPath = path.substringBefore("/Android/data")
                val rootFile = File(rootPath)
                if (rootFile.exists() && drives.none { it.path == rootFile.absolutePath }) {
                    drives.add(
                        StorageDrive(
                            name = "USB Drive (${rootFile.name})",
                            path = rootFile.absolutePath,
                            isUsb = true,
                            totalSpace = rootFile.totalSpace,
                            freeSpace = rootFile.freeSpace
                        )
                    )
                }
            }
        }

        // 4. Also inspect /storage/ directly for mounted OTG drives
        val storageRoot = File("/storage")
        if (storageRoot.exists() && storageRoot.isDirectory) {
            val children = storageRoot.listFiles()
            if (children != null) {
                for (child in children) {
                    if (child.name != "emulated" && child.name != "self" && child.canRead() && drives.none { it.path == child.absolutePath }) {
                        drives.add(
                            StorageDrive(
                                name = "External (${child.name})",
                                path = child.absolutePath,
                                isUsb = true,
                                totalSpace = child.totalSpace,
                                freeSpace = child.freeSpace
                            )
                        )
                    }
                }
            }
        }

        drives
    }
}
