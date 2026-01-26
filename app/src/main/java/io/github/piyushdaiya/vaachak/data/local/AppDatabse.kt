package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HighlightEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao
}