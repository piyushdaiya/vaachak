package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
@Database(entities = [HighlightEntity::class, BookEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao
    abstract fun bookDao(): BookDao
}