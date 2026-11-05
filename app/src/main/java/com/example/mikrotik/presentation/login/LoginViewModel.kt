package com.example.mikrotik.presentation.login

import androidx.lifecycle.ViewModel
import com.example.mikrotik.core.captive.CaptivePortalServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val captivePortalServer: CaptivePortalServer
) : ViewModel() {

    private val _serverUrl = MutableStateFlow(captivePortalServer.getLocalAccessUrl())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _isServerRunning = MutableStateFlow(captivePortalServer.isServerRunning())
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    init {
        if (!captivePortalServer.isServerRunning()) {
            captivePortalServer.startServer()
            _isServerRunning.value = captivePortalServer.isServerRunning()
            _serverUrl.value = captivePortalServer.getLocalAccessUrl()
        }
    }
}
