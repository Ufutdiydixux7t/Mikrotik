package com.example.mikrotik.data.db.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey
    val code: String,
    val durationMinutes: Long,
    val dataLimitBytes: Long,
    val boundMacAddress: String? = null,
    val isUsed: Boolean = false,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val firstUsedAt: Long? = null,
    val bytesUsedSoFar: Long = 0L,
    @ColumnInfo(defaultValue = "'LEGACY'")
    val packageType: String = "LEGACY"
)
