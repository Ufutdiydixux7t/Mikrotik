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

    @Query("SELECT * FROM vouchers WHERE boundMacAddress = :macAddress LIMIT 1")
    suspend fun getVoucherByMac(macAddress: String): VoucherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVouchers(vouchers: List<VoucherEntity>)

    @Update
    suspend fun updateVoucher(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE code = :code")
    suspend fun deleteVoucher(code: String)
}
