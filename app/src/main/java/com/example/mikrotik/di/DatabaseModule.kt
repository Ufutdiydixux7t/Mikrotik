package com.example.mikrotik.di

import android.content.Context
import androidx.room.Room
import com.example.mikrotik.data.db.AppDatabase
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.dao.VoucherDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideVoucherDao(database: AppDatabase): VoucherDao = database.voucherDao()

    @Provides
    fun provideActiveSessionDao(database: AppDatabase): ActiveSessionDao = database.activeSessionDao()

    @Provides
    fun provideBlockedMacDao(database: AppDatabase): BlockedMacDao = database.blockedMacDao()
}
