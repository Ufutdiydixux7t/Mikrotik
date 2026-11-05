package com.example.mikrotik.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_sessions")
data class ActiveSessionEntity(
    @PrimaryKey
    val macAddress: String,
    val ipAddress: String,
    val voucherCode: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val bytesDownloaded: Long = 0L,
    val bytesUploaded: Long = 0L,
    val isActive: Boolean = true
)
