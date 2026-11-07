package com.example.mikrotik.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mikrotik.data.db.entity.BlockedMacEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedMacDao {

    @Query("SELECT * FROM blocked_macs ORDER BY blockedAt DESC")
    fun getBlockedMacsFlow(): Flow<List<BlockedMacEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_macs WHERE macAddress COLLATE NOCASE = :macAddress)")
    suspend fun isMacBlocked(macAddress: String): Boolean

    @Query("SELECT * FROM blocked_macs ORDER BY blockedAt DESC")
    suspend fun getBlockedMacsSnapshot(): List<BlockedMacEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockMac(blockedMac: BlockedMacEntity)

    @Query("DELETE FROM blocked_macs WHERE macAddress COLLATE NOCASE = :macAddress")
    suspend fun unblockMac(macAddress: String)
}
