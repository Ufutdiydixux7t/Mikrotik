package com.example.mikrotik.presentation.diagnostics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.captive.PortalAppearance
import com.example.mikrotik.core.captive.PortalAppearanceSettings
import com.example.mikrotik.core.captive.PortalTheme
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.core.root.RootDiagnostics
import com.example.mikrotik.core.root.SystemDiagnosticReport
import com.example.mikrotik.data.db.dao.VoucherDao
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DiagnosticsUiState {
    object Loading : DiagnosticsUiState()
    data class Success(
        val report: SystemDiagnosticReport,
        val databaseStatus: String
    ) : DiagnosticsUiState()
    data class Error(val message: String) : DiagnosticsUiState()
}

data class PhoneInternetBlockUiState(
    val isEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val failed: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootDiagnostics: RootDiagnostics,
    private val voucherDao: VoucherDao,
    private val portalAppearanceSettings: PortalAppearanceSettings,
    private val iptablesManager: IptablesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiagnosticsUiState>(DiagnosticsUiState.Loading)
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    private val _portalAppearance = MutableStateFlow(portalAppearanceSettings.current())
    val portalAppearance: StateFlow<PortalAppearance> = _portalAppearance.asStateFlow()

    private val _phoneInternetBlock = MutableStateFlow(PhoneInternetBlockUiState())
    val phoneInternetBlock: StateFlow<PhoneInternetBlockUiState> = _phoneInternetBlock.asStateFlow()

    init {
        runDiagnostics()
        refreshPhoneInternetBlockStatus()
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _uiState.value = DiagnosticsUiState.Loading
            try {
                val report = rootDiagnostics.runSafeDiagnostics()
                
                // Test database connectivity by reading vouchers flow count
                val dbStatus = try {
                    voucherDao.getVoucherByCode("test_code")
                    "Room Database Connected & Operational"
                } catch (e: Exception) {
                    "DB Error: ${e.message}"
                }

                _uiState.value = DiagnosticsUiState.Success(
                    report = report,
                    databaseStatus = dbStatus
                )
            } catch (e: Exception) {
                _uiState.value = DiagnosticsUiState.Error(
                    message = e.message ?: "Failed to execute diagnostics"
                )
            }
        }
    }

    fun updatePortalName(name: String) {
        portalAppearanceSettings.saveName(name)
        _portalAppearance.value = _portalAppearance.value.copy(
            name = name.take(PortalAppearanceSettings.MAX_NAME_LENGTH)
        )
    }

    fun updatePortalTheme(theme: PortalTheme) {
        portalAppearanceSettings.saveTheme(theme)
        _portalAppearance.value = portalAppearanceSettings.current()
    }

    fun refreshPhoneInternetBlockStatus() {
        viewModelScope.launch {
            val status = iptablesManager.isPhoneAppsInternetBlocked(context.applicationInfo.uid)
            _phoneInternetBlock.value = PhoneInternetBlockUiState(
                isEnabled = status ?: _phoneInternetBlock.value.isEnabled,
                isLoading = false,
                failed = status == null
            )
        }
    }

    fun setPhoneAppsInternetBlocked(enabled: Boolean) {
        if (_phoneInternetBlock.value.isLoading) return
        viewModelScope.launch {
            _phoneInternetBlock.value = _phoneInternetBlock.value.copy(isLoading = true, failed = false)
            val applied = iptablesManager.setPhoneAppsInternetBlocked(
                appUid = context.applicationInfo.uid,
                blocked = enabled
            )
            val status = iptablesManager.isPhoneAppsInternetBlocked(context.applicationInfo.uid)
            _phoneInternetBlock.value = PhoneInternetBlockUiState(
                isEnabled = status ?: _phoneInternetBlock.value.isEnabled,
                isLoading = false,
                failed = !applied || status == null || status != enabled
            )
        }
    }
}
