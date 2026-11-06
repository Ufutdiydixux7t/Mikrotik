package com.example.mikrotik.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.captive.PortalAppearance
import com.example.mikrotik.core.captive.PortalAppearanceSettings
import com.example.mikrotik.core.captive.PortalTheme
import com.example.mikrotik.core.root.RootDiagnostics
import com.example.mikrotik.core.root.SystemDiagnosticReport
import com.example.mikrotik.data.db.dao.VoucherDao
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

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val rootDiagnostics: RootDiagnostics,
    private val voucherDao: VoucherDao,
    private val portalAppearanceSettings: PortalAppearanceSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiagnosticsUiState>(DiagnosticsUiState.Loading)
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    private val _portalAppearance = MutableStateFlow(portalAppearanceSettings.current())
    val portalAppearance: StateFlow<PortalAppearance> = _portalAppearance.asStateFlow()

    init {
        runDiagnostics()
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
}
