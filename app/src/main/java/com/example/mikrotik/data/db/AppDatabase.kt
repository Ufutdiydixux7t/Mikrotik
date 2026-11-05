package com.example.mikrotik.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun voucherDao(): VoucherDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun blockedMacDao(): BlockedMacDao

    companion object {
        const val DATABASE_NAME = "mini_mikrotik_db"
    }
}
