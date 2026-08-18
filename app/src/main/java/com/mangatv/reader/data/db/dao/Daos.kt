package com.mangatv.reader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mangatv.reader.data.db.entity.BookmarkedDirectoryEntity
import com.mangatv.reader.data.db.entity.ComicProgressEntity
import com.mangatv.reader.data.db.entity.SmbShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Query("SELECT * FROM comic_progress ORDER BY lastReadTimestamp DESC")
    fun getAllHistory(): Flow<List<ComicProgressEntity>>

    @Query("SELECT * FROM comic_progress ORDER BY lastReadTimestamp DESC")
    suspend fun getAllHistoryList(): List<ComicProgressEntity>

    @Query("SELECT * FROM comic_progress WHERE path = :path LIMIT 1")
    suspend fun getProgressForPath(path: String): ComicProgressEntity?

    @Query("SELECT * FROM comic_progress WHERE currentPage > 0 AND isCompleted = 0 ORDER BY lastReadTimestamp DESC LIMIT :limit")
    fun getRecentUnfinished(limit: Int = 10): Flow<List<ComicProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: ComicProgressEntity)

    @Query("DELETE FROM comic_progress WHERE path = :path")
    suspend fun deleteByPath(path: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarked_directories ORDER BY addedTimestamp ASC")
    fun getAllBookmarks(): Flow<List<BookmarkedDirectoryEntity>>

    @Query("SELECT * FROM bookmarked_directories ORDER BY addedTimestamp ASC")
    suspend fun getAllBookmarksList(): List<BookmarkedDirectoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedDirectoryEntity)

    @Query("DELETE FROM bookmarked_directories WHERE path = :path")
    suspend fun deleteBookmark(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_directories WHERE path = :path)")
    suspend fun isBookmarked(path: String): Boolean
}

@Dao
interface SmbShareDao {
    @Query("SELECT * FROM smb_shares ORDER BY id ASC")
    fun getAllShares(): Flow<List<SmbShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: SmbShareEntity): Long

    @Query("DELETE FROM smb_shares WHERE id = :id")
    suspend fun deleteShare(id: Long)
}
