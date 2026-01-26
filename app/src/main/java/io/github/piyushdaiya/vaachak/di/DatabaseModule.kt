package io.github.piyushdaiya.vaachak.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.piyushdaiya.vaachak.data.local.AppDatabase
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vaachak_db"
        ).build()
    }

    @Provides
    fun provideHighlightDao(database: AppDatabase): HighlightDao {
        return database.highlightDao()
    }
}

