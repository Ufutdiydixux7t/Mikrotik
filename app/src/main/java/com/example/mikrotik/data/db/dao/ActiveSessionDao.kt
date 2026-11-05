package com.example.mikrotik.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mikrotik.data.db.entity.ActiveSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {

    @Query("SELECT * FROM active_sessions WHERE isActive = 1 ORDER BY connectedAt DESC")
    fun getActiveSessionsFlow(): Flow<List<ActiveSessionEntity>>

    @Query("SELECT * FROM active_sessions WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getSessionByMac(macAddress: String): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: ActiveSessionEntity)

    @Update
    suspend fun updateSession(session: ActiveSessionEntity)

    @Query("UPDATE active_sessions SET isActive = 0 WHERE macAddress = :macAddress")
    suspend fun deactivateSession(macAddress: String)

    @Query("DELETE FROM active_sessions WHERE macAddress = :macAddress")
    suspend fun deleteSession(macAddress: String)
}
