package com.example.menstruation.di

import android.content.Context
import androidx.room.Room
import com.example.menstruation.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "menstruation.db"
        ).build()
    }

    @Provides
    fun provideDailyRecordDao(database: AppDatabase) = database.dailyRecordDao()

    @Provides
    fun providePeriodDao(database: AppDatabase) = database.periodDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
}
