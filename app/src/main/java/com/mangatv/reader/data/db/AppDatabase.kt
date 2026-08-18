package com.mangatv.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mangatv.reader.data.db.dao.BookmarkDao
import com.mangatv.reader.data.db.dao.ComicDao
import com.mangatv.reader.data.db.dao.SmbShareDao
import com.mangatv.reader.data.db.entity.BookmarkedDirectoryEntity
import com.mangatv.reader.data.db.entity.ComicProgressEntity
import com.mangatv.reader.data.db.entity.SmbShareEntity

@Database(
    entities = [
        ComicProgressEntity::class,
        BookmarkedDirectoryEntity::class,
        SmbShareEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun smbShareDao(): SmbShareDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mangatv_reader.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
