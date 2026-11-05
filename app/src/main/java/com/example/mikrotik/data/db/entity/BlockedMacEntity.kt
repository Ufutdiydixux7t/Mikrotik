package com.example.mikrotik.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_macs")
data class BlockedMacEntity(
    @PrimaryKey
    val macAddress: String,
    val reason: String = "Manual Block",
    val blockedAt: Long = System.currentTimeMillis()
)
