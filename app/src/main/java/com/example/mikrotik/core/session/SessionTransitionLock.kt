package com.example.mikrotik.core.session

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes voucher-login and session-enforcement transitions across app components. */
@Singleton
class SessionTransitionLock @Inject constructor() {
    private val mutex = Mutex()

    suspend fun lock() = mutex.lock()

    fun unlock() = mutex.unlock()
}
