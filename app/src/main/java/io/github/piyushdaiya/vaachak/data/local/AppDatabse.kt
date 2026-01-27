package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities = [HighlightEntity::class, BookEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao
    abstract fun bookDao(): BookDao
    companion object {
        const val DATABASE_NAME = "vaachak_db"

        // Migration from version 4 to 5 to add lastLocationJson
        val MIGRATION_4_5 = object : Migration(4,5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add lastLocationJson (Nullable TEXT)
                db.execSQL("ALTER TABLE books ADD COLUMN lastLocationJson TEXT")

                // 2. Add addedDate (Non-null INTEGER)
                db.execSQL("ALTER TABLE books ADD COLUMN addedDate INTEGER NOT NULL DEFAULT 0")

            }
        }
    }
}