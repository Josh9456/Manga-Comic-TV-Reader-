package com.mangatv.reader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comic_progress")
data class ComicProgressEntity(
    @PrimaryKey
    val path: String,
    val title: String,
    val series: String? = null,
    val number: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val readingMode: String = "RTL",
    val aspectMode: String = "FIT_SCREEN",
    val spreadMode: String = "DUAL_PAGE",
    val coverPath: String? = null
)

@Entity(tableName = "bookmarked_directories")
data class BookmarkedDirectoryEntity(
    @PrimaryKey
    val path: String,
    val displayName: String,
    val source: String = "LOCAL", // "LOCAL", "USB", "SMB"
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "smb_shares")
data class SmbShareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val host: String,
    val shareName: String,
    val username: String = "",
    val domain: String = "",
    val passwordEncrypted: String = "",
    val port: Int = 445
)
