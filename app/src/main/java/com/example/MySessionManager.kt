package com.example

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPreferencesSessionManager(context: Context) : SessionManager {
    private val sharedPrefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
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
        val sessionString = sharedPrefs.getString("session", null) ?: return null
        return try {
            json.decodeFromString<UserSession>(sessionString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun deleteSession() {
        sharedPrefs.edit().remove("session").apply()
    }
}


