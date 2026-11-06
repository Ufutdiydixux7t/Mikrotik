package com.example.mikrotik.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.captive.CaptivePortalServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val captivePortalServer: CaptivePortalServer
) : ViewModel() {

    private val _isServerRunning = MutableStateFlow(captivePortalServer.isServerRunning())
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIp = MutableStateFlow(captivePortalServer.getActiveHotspotIpAddress())
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(captivePortalServer.getPort())
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _serverUrl = MutableStateFlow(captivePortalServer.getLocalAccessUrl())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _healthTestResult = MutableStateFlow<String?>(null)
    val healthTestResult: StateFlow<String?> = _healthTestResult.asStateFlow()

    init {
        checkAndStartServer()
    }

    fun checkAndStartServer() {
        viewModelScope.launch {
            if (!captivePortalServer.isServerRunning()) {
                captivePortalServer.startServer()
            }
            _isServerRunning.value = captivePortalServer.isServerRunning()
            _serverIp.value = captivePortalServer.getActiveHotspotIpAddress()
            _serverPort.value = captivePortalServer.getPort()
            _serverUrl.value = captivePortalServer.getLocalAccessUrl()
        }
    }

    fun testServerHealth() {
        viewModelScope.launch {
            _healthTestResult.value = "جاري فحص مسار الصحة (/health)..."
            val urlStr = "${captivePortalServer.getLocalAccessUrl()}/health"
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.requestMethod = "GET"
                    val code = conn.responseCode
                    if (code == 200) {
                        "السيرفر يعمل بنجاح! (HTTP 200 OK - /health)"
                    } else {
                        "استجاب السيرفر برمز: $code"
                    }
                } catch (e: Exception) {
                    "فشل الاتصال: ${e.message}"
                }
            }
            _healthTestResult.value = result
        }
    }
}
