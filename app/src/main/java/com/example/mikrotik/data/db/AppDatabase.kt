package com.example.mikrotik.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.ActiveSessionEntity
import com.example.mikrotik.data.db.entity.BlockedMacEntity
import com.example.mikrotik.data.db.entity.VoucherEntity

@Database(
    entities = [
        VoucherEntity::class,
        ActiveSessionEntity::class,
        BlockedMacEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun voucherDao(): VoucherDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun blockedMacDao(): BlockedMacDao

    companion object {
        const val DATABASE_NAME = "mini_mikrotik_db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `vouchers` ADD COLUMN `packageType` TEXT NOT NULL DEFAULT 'LEGACY'"
                )
            }
        }
    }
}
