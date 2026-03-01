package com.remnd.di

import android.content.Context
import androidx.room.Room
import com.remnd.data.ReminderDao
import com.remnd.data.ReminderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideReminderDatabase(@ApplicationContext context: Context): ReminderDatabase =
        Room.databaseBuilder(
            context,
            ReminderDatabase::class.java,
            ReminderDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideReminderDao(database: ReminderDatabase): ReminderDao =
        database.reminderDao()
}
