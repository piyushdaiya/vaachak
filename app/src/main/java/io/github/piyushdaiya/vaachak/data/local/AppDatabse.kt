/*
 *  Copyright (c) 2026 Piyush Daiya
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 */

package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities = [HighlightEntity::class, BookEntity::class, OpdsEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao
    abstract fun bookDao(): BookDao
    abstract fun opdsDao(): OpdsDao // Add this
    companion object {
        const val DATABASE_NAME = "vaachak_db"
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQL to create the new table exactly as Room expects it
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `opds_feeds` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `url` TEXT NOT NULL, 
                        `username` TEXT, 
                        `password` TEXT, 
                        `isPredefined` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }
        // NEW Migration (2->3)
        val MIGRATION_8_9= object : Migration(8,9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new column with a default value of 0 (false)
                database.execSQL("ALTER TABLE `opds_feeds` ADD COLUMN `allowInsecure` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
