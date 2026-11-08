package com.example.mikrotik.security

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val repository = AppLockRepository(context)
    private val _config = MutableStateFlow(repository.loadConfig())
    val config: StateFlow<AppLockConfig> = _config.asStateFlow()

    private val _isLocked = MutableStateFlow(_config.value.enabled)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _retryAfterMillis = MutableStateFlow(0L)
    val retryAfterMillis: StateFlow<Long> = _retryAfterMillis.asStateFlow()

    private var lastBackgroundAt: Long? = null
    private var screenLockPending = false

    fun onAppBackground(isScreenLocked: Boolean, now: Long = SystemClock.elapsedRealtime()) {
        if (!_config.value.enabled) return
        lastBackgroundAt = now
        when (_config.value.policy) {
            AppLockPolicy.ON_APP_EXIT -> _isLocked.value = true
            AppLockPolicy.ON_SCREEN_LOCK -> {
                if (isScreenLocked) {
                    screenLockPending = true
                    _isLocked.value = true
                }
            }
            AppLockPolicy.AFTER_ONE_MINUTE,
            AppLockPolicy.AFTER_FIVE_MINUTES -> Unit
        }
    }

    fun onScreenLocked() {
        if (_config.value.enabled && _config.value.policy == AppLockPolicy.ON_SCREEN_LOCK) {
            screenLockPending = true
            _isLocked.value = true
        }
    }

    fun onAppForeground(now: Long = SystemClock.elapsedRealtime()) {
        if (!_config.value.enabled) {
            _isLocked.value = false
            lastBackgroundAt = null
            screenLockPending = false
            return
        }
        val backgroundAt = lastBackgroundAt
        when (_config.value.policy) {
            AppLockPolicy.ON_APP_EXIT -> if (backgroundAt != null) _isLocked.value = true
            AppLockPolicy.ON_SCREEN_LOCK -> if (screenLockPending) _isLocked.value = true
            AppLockPolicy.AFTER_ONE_MINUTE,
            AppLockPolicy.AFTER_FIVE_MINUTES -> {
                val timeout = _config.value.policy.timeoutMillis ?: Long.MAX_VALUE
                if (backgroundAt != null && now - backgroundAt >= timeout) _isLocked.value = true
            }
        }
        lastBackgroundAt = null
        screenLockPending = false
    }

    fun saveConfiguration(names: List<String>, pins: List<String>, policy: AppLockPolicy, onComplete: (Boolean) -> Unit) {
        if (_isVerifying.value) return
        _isVerifying.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    repository.saveConfiguration(names, pins, policy)
                }
            } catch (_: Exception) {
                false
            }
            if (saved) {
                _config.value = repository.loadConfig()
                _isLocked.value = false
                lastBackgroundAt = null
                screenLockPending = false
            } else {
                _errorMessage.value = "تعذر حفظ الإعدادات؛ تحقق من الأسماء ورموز PIN المختلفة."
            }
            _isVerifying.value = false
            onComplete(saved)
        }
    }

    fun disableLock(onComplete: (Boolean) -> Unit) {
        if (_isVerifying.value) return
        _isVerifying.value = true
        viewModelScope.launch {
            val disabled = try {
                withContext(Dispatchers.IO) { repository.disable() }
            } catch (_: Exception) {
                false
            }
            if (disabled) {
                _config.value = repository.loadConfig()
                _isLocked.value = false
                lastBackgroundAt = null
                screenLockPending = false
                _errorMessage.value = null
            } else {
                _errorMessage.value = "تعذر إيقاف القفل؛ أعد المحاولة."
            }
            _isVerifying.value = false
            onComplete(disabled)
        }
    }

    fun updatePolicy(policy: AppLockPolicy, onComplete: (Boolean) -> Unit) {
        if (_isVerifying.value) return
        _isVerifying.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val updated = try {
                withContext(Dispatchers.IO) { repository.updatePolicy(policy) }
            } catch (_: Exception) {
                false
            }
            if (updated) {
                _config.value = repository.loadConfig()
                _errorMessage.value = null
            } else {
                _errorMessage.value = "تعذر حفظ سياسة القفل؛ أعد المحاولة."
            }
            _isVerifying.value = false
            onComplete(updated)
        }
    }

    fun unlock(userIndex: Int, pin: String) {
        verify(userIndex, pin) { verified ->
            if (verified) _isLocked.value = false
        }
    }

    fun authorizeSettings(userIndex: Int, pin: String, onVerified: (Boolean) -> Unit) {
        verify(userIndex, pin, onVerified)
    }

    private fun verify(userIndex: Int, pin: String, onVerified: (Boolean) -> Unit) {
        if (_isVerifying.value) return
        _isVerifying.value = true
        _errorMessage.value = null
        _retryAfterMillis.value = 0L
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.verify(userIndex, pin) }
            if (result.verified) {
                _errorMessage.value = null
                _retryAfterMillis.value = 0L
            } else if (result.retryAfterMillis > 0L) {
                _errorMessage.value = "محاولات كثيرة. حاول مجدداً بعد قليل."
                _retryAfterMillis.value = result.retryAfterMillis
            } else {
                _errorMessage.value = "رمز PIN غير صحيح. تحقق من المستخدم والرمز."
            }
            _isVerifying.value = false
            onVerified(result.verified)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
