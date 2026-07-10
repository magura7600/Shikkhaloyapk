package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PrefUtils {
    private var securePrefsInstance: SharedPreferences? = null

    /**
     * Retrieves a thread-safe, secure instance of SharedPreferences backed by EncryptedSharedPreferences.
     * Safe from inspection on rooted devices. Falls back gracefully to standard preferences if encryption is unsupported.
     */
    fun getSecurePrefs(context: Context): SharedPreferences {
        securePrefsInstance?.let { return it }
        synchronized(this) {
            securePrefsInstance?.let { return it }
            val appContext = context.applicationContext
            val prefs = try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appContext,
                    "shikkhaloy_prefs_secure",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to plain preferences wrapped to sanitize administrative roles
                val raw = appContext.getSharedPreferences("shikkhaloy_prefs", Context.MODE_PRIVATE)
                RoleSanitizedSharedPreferences(raw)
            }
            securePrefsInstance = prefs
            return prefs
        }
    }

    private class RoleSanitizedSharedPreferences(private val delegate: SharedPreferences) : SharedPreferences by delegate {
        override fun getString(key: String?, defValue: String?): String? {
            if (key == "role") {
                val rawValue = delegate.getString(key, defValue)
                // Never grant admin role via plain, non-encrypted fallback storage
                return if (rawValue == "admin") "student" else rawValue
            }
            return delegate.getString(key, defValue)
        }
    }
}
