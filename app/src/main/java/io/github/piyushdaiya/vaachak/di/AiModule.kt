package io.github.piyushdaiya.vaachak.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.piyushdaiya.vaachak.AppConfig
import io.github.piyushdaiya.vaachak.data.api.CloudflareAiApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideCloudflareAiApi(): CloudflareAiApi {
        return Retrofit.Builder()
            .baseUrl(AppConfig.CLOUDFLARE_WORKER_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudflareAiApi::class.java)
    }
}

