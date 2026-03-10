package com.zhiqi.app.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val _pinConfigured = MutableStateFlow(prefs.contains(KEY_PIN_HASH))
    private val _passwordEnabled = MutableStateFlow(readPasswordEnabled())
    val pinConfigured = _pinConfigured.asStateFlow()
    val passwordEnabled = _passwordEnabled.asStateFlow()

    fun isPinSet(): Boolean = _pinConfigured.value

    fun isPasswordEnabled(): Boolean = _passwordEnabled.value

    fun setPasswordEnabled(enabled: Boolean) {
        val editor = prefs.edit().putBoolean(KEY_PASSWORD_ENABLED, enabled)
        if (!enabled) {
            editor.putInt(KEY_FAIL_COUNT, 0).putBoolean(KEY_HIDDEN, false)
        }
        editor.apply()
        _passwordEnabled.value = enabled
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putInt(KEY_FAIL_COUNT, 0)
            .putBoolean(KEY_HIDDEN, false)
            .apply()
        _pinConfigured.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val actual = hashPin(pin, salt)
        val ok = expected.contentEquals(actual)

        if (ok) {
            prefs.edit().putInt(KEY_FAIL_COUNT, 0).putBoolean(KEY_HIDDEN, false).apply()
        } else {
            val fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAIL_COUNT, fails)
            if (fails >= 5) {
                editor.putBoolean(KEY_HIDDEN, true)
            }
            editor.apply()
        }
        return ok
    }

    fun isHidden(): Boolean = prefs.getBoolean(KEY_HIDDEN, false)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun exportSnapshot(): PinSnapshot {
        return PinSnapshot(
            hashBase64 = prefs.getString(KEY_PIN_HASH, null),
            saltBase64 = prefs.getString(KEY_PIN_SALT, null),
            failCount = prefs.getInt(KEY_FAIL_COUNT, 0),
            hidden = prefs.getBoolean(KEY_HIDDEN, false),
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
            passwordEnabled = isPasswordEnabled()
        )
    }

    fun restoreSnapshot(snapshot: PinSnapshot?) {
        if (snapshot == null || snapshot.hashBase64.isNullOrBlank() || snapshot.saltBase64.isNullOrBlank()) {
            clearAll()
            return
        }
        prefs.edit()
            .putString(KEY_PIN_HASH, snapshot.hashBase64)
            .putString(KEY_PIN_SALT, snapshot.saltBase64)
            .putInt(KEY_FAIL_COUNT, snapshot.failCount.coerceAtLeast(0))
            .putBoolean(KEY_HIDDEN, snapshot.hidden)
            .putBoolean(KEY_BIOMETRIC, snapshot.biometricEnabled)
            .putBoolean(KEY_PASSWORD_ENABLED, snapshot.passwordEnabled)
            .apply()
        _pinConfigured.value = true
        _passwordEnabled.value = snapshot.passwordEnabled
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _pinConfigured.value = false
        _passwordEnabled.value = false
    }

    private fun readPasswordEnabled(): Boolean {
        if (!prefs.contains(KEY_PASSWORD_ENABLED) && _pinConfigured.value) {
            prefs.edit().putBoolean(KEY_PASSWORD_ENABLED, true).apply()
            return true
        }
        return prefs.getBoolean(KEY_PASSWORD_ENABLED, false)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_secure"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAIL_COUNT = "pin_fail_count"
        private const val KEY_HIDDEN = "records_hidden"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_PASSWORD_ENABLED = "password_enabled"
    }
}

data class PinSnapshot(
    val hashBase64: String?,
    val saltBase64: String?,
    val failCount: Int,
    val hidden: Boolean,
    val biometricEnabled: Boolean,
    val passwordEnabled: Boolean = false
)
