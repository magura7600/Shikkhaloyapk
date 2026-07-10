package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPreferencesSessionManager(context: Context) : SessionManager {
    
    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            EncryptedSharedPreferences.create(
                context,
                "supabase_session_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to legacysharedpreferences on encryption initialization failure
            context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
        }
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun saveSession(session: UserSession) {
        try {
            val sessionString = json.encodeToString(session)
            sharedPrefs.edit().putString("session", sessionString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadSession(): UserSession? {
        return try {
            val sessionString = sharedPrefs.getString("session", null) ?: return null
            json.decodeFromString<UserSession>(sessionString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun deleteSession() {
        try {
            sharedPrefs.edit().remove("session").apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


