package com.example.mikrotik.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mikrotik.data.db.entity.VoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {

    @Query("SELECT * FROM vouchers ORDER BY createdAt DESC")
    fun getAllVouchersFlow(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE code = :code LIMIT 1")
    suspend fun getVoucherByCode(code: String): VoucherEntity?

    @Query("UPDATE vouchers SET isBlocked = 1 WHERE code = :code")
    suspend fun blockVoucherForDeletion(code: String): Int

    @Query(
        """UPDATE vouchers
           SET isUsed = 1,
               firstUsedAt = COALESCE(firstUsedAt, :now),
               boundMacAddress = COALESCE(boundMacAddress, :macAddress)
           WHERE code = :code
             AND isBlocked = 0
             AND bytesUsedSoFar < dataLimitBytes
             AND (boundMacAddress IS NULL OR boundMacAddress COLLATE NOCASE = :macAddress)
             AND (firstUsedAt IS NULL OR :now < firstUsedAt + durationMinutes * 60000)"""
    )
    suspend fun activateVoucherIfValid(code: String, macAddress: String, now: Long): Int

    @Query("SELECT * FROM vouchers WHERE boundMacAddress COLLATE NOCASE = :macAddress LIMIT 1")
    suspend fun getVoucherByMac(macAddress: String): VoucherEntity?

    @Query("SELECT * FROM vouchers WHERE boundMacAddress COLLATE NOCASE = :macAddress")
    suspend fun getVouchersByMac(macAddress: String): List<VoucherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVouchers(vouchers: List<VoucherEntity>)

    @Update
    suspend fun updateVoucher(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE code = :code")
    suspend fun deleteVoucher(code: String)
}
