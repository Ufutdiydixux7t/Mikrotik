package com.example.mikrotik.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.entity.ActiveSessionEntity
import com.example.mikrotik.data.db.entity.BlockedMacEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val activeSessionDao: ActiveSessionDao,
    private val blockedMacDao: BlockedMacDao,
    private val iptablesManager: IptablesManager
) : ViewModel() {

    val activeSessions: StateFlow<List<ActiveSessionEntity>> = activeSessionDao.getActiveSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedMacs: StateFlow<List<BlockedMacEntity>> = blockedMacDao.getBlockedMacsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun blockDevice(macAddress: String, reason: String = "حظر يدوياً من لوحة التحكم") {
        viewModelScope.launch {
            iptablesManager.blockMacAddress(macAddress)
            blockedMacDao.blockMac(BlockedMacEntity(macAddress = macAddress, reason = reason))
            activeSessionDao.deactivateSession(macAddress)
        }
    }

    fun unblockDevice(macAddress: String) {
        viewModelScope.launch {
            iptablesManager.unblockMacAddress(macAddress)
            blockedMacDao.unblockMac(macAddress)
        }
    }
}
