package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities = [HighlightEntity::class, BookEntity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao
    abstract fun bookDao(): BookDao

    companion object {
        const val DATABASE_NAME = "vaachak_db"
    }

}