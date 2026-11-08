package com.example.mikrotik.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal data class StoredAppLockUser(
    val name: String,
    val salt: String,
    val verifier: String
)

enum class AppLockPolicy(val key: String, val timeoutMillis: Long?) {
    ON_SCREEN_LOCK("screen_lock", null),
    ON_APP_EXIT("app_exit", 0L),
    AFTER_ONE_MINUTE("one_minute", 60_000L),
    AFTER_FIVE_MINUTES("five_minutes", 5 * 60_000L);

    companion object {
        fun fromKey(key: String?): AppLockPolicy = entries.firstOrNull { it.key == key } ?: ON_APP_EXIT
    }
}

data class AppLockConfig(
    val enabled: Boolean = false,
    val userNames: List<String> = emptyList(),
    val policy: AppLockPolicy = AppLockPolicy.ON_APP_EXIT
)

data class PinVerificationResult(
    val verified: Boolean,
    val retryAfterMillis: Long = 0L
)

/** Stores PIN verifiers (never plaintext PINs) in app-private preferences. */
class AppLockRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun loadConfig(): AppLockConfig {
        val enabled = preferences.getBoolean(KEY_ENABLED, false)
        val count = preferences.getInt(KEY_USER_COUNT, 0).coerceIn(0, 3)
        val names = (0 until count).mapNotNull { index ->
            preferences.getString(userKey(index, "name"), null)?.takeIf { it.isNotBlank() }
        }
        return AppLockConfig(
            enabled = enabled && names.isNotEmpty(),
            userNames = names,
            policy = AppLockPolicy.fromKey(preferences.getString(KEY_POLICY, null))
        )
    }

    @Synchronized
    fun saveConfiguration(names: List<String>, pins: List<String>, policy: AppLockPolicy): Boolean {
        if (names.size !in setOf(1, 3) || pins.size != names.size) return false
        val cleanNames = names.map { it.trim() }
        if (cleanNames.any { it.isEmpty() } || cleanNames.map { it.lowercase() }.distinct().size != cleanNames.size) return false
        if (pins.any { !it.matches(Regex("^[0-9]{6,12}$")) } || pins.distinct().size != pins.size) return false

        val users = cleanNames.indices.map { index ->
            val saltBytes = ByteArray(SALT_SIZE).also(random::nextBytes)
            val pin = pins[index]
            StoredAppLockUser(
                name = cleanNames[index],
                salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP),
                verifier = Base64.encodeToString(derive(pin, saltBytes), Base64.NO_WRAP)
            )
        }

        preferences.edit().apply {
            putBoolean(KEY_ENABLED, true)
            putInt(KEY_USER_COUNT, users.size)
            putString(KEY_POLICY, policy.key)
            users.forEachIndexed { index, user ->
                putString(userKey(index, "name"), user.name)
                putString(userKey(index, "salt"), user.salt)
                putString(userKey(index, "verifier"), user.verifier)
            }
            for (index in users.size..2) {
                remove(userKey(index, "name"))
                remove(userKey(index, "salt"))
                remove(userKey(index, "verifier"))
            }
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL)
        }.commit()
    }

    @Synchronized
    fun disable(): Boolean = preferences.edit().clear().commit()

    @Synchronized
    fun updatePolicy(policy: AppLockPolicy): Boolean {
        if (!preferences.getBoolean(KEY_ENABLED, false)) return false
        return preferences.edit().putString(KEY_POLICY, policy.key).commit()
    }

    @Synchronized
    fun verify(userIndex: Int, pin: String): PinVerificationResult {
        val now = System.currentTimeMillis()
        val lockoutUntil = preferences.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (now < lockoutUntil) return PinVerificationResult(false, lockoutUntil - now)

        val count = preferences.getInt(KEY_USER_COUNT, 0)
        val validIndex = userIndex in 0 until count
        val salt = if (validIndex) preferences.getString(userKey(userIndex, "salt"), null) else null
        val expected = if (validIndex) preferences.getString(userKey(userIndex, "verifier"), null) else null
        val verified = try {
            if (salt == null || expected == null || !pin.matches(Regex("^[0-9]{6,12}$"))) {
                false
            } else {
                val actual = derive(pin, Base64.decode(salt, Base64.NO_WRAP))
                MessageDigest.isEqual(actual, Base64.decode(expected, Base64.NO_WRAP))
            }
        } catch (_: Exception) {
            false
        }

        if (verified) {
            preferences.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_UNTIL).apply()
            return PinVerificationResult(true)
        }

        val attempts = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val lockDuration = when {
            attempts >= 15 -> 5 * 60_000L
            attempts >= 10 -> 2 * 60_000L
            attempts >= 5 -> 30_000L
            else -> 0L
        }
        preferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_LOCKOUT_UNTIL, if (lockDuration > 0L) now + lockDuration else 0L)
            .apply()
        return PinVerificationResult(false, lockDuration)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val password = pin.toCharArray()
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun userKey(index: Int, field: String) = "user_${index}_$field"

    private companion object {
        const val PREFERENCES_NAME = "app_lock_credentials"
        const val KEY_ENABLED = "enabled"
        const val KEY_USER_COUNT = "user_count"
        const val KEY_POLICY = "policy"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKOUT_UNTIL = "lockout_until"
        const val SALT_SIZE = 16
        const val PBKDF2_ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
