package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PrefUtils {
    private var securePrefsInstance: SharedPreferences? = null

    fun getSecurePrefs(context: Context): SharedPreferences {
        securePrefsInstance?.let { return it }
        synchronized(this) {
            securePrefsInstance?.let { return it }
            val appContext = context.applicationContext
            val prefs = try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val encPrefs = EncryptedSharedPreferences.create(
                    appContext,
                    "shikkhaloy_prefs_secure",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                SafeSharedPreferences(encPrefs, appContext)
            } catch (e: Throwable) {
                Log.e("Shikkhaloy", "Failed to initialize EncryptedSharedPreferences, falling back to raw preferences", e)
                val raw = appContext.getSharedPreferences("shikkhaloy_prefs", Context.MODE_PRIVATE)
                RoleSanitizedSharedPreferences(raw)
            }
            securePrefsInstance = prefs
            return prefs
        }
    }

    private class SafeSharedPreferences(private val delegate: SharedPreferences, private val context: Context) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = try { delegate.all } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getAll prefs", e); mutableMapOf<String, Any>() }
        override fun getString(key: String?, defValue: String?): String? = try { delegate.getString(key, defValue) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getString pref for key $key", e); defValue }
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = try { delegate.getStringSet(key, defValues) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getStringSet pref for key $key", e); defValues }
        override fun getInt(key: String?, defValue: Int): Int = try { delegate.getInt(key, defValue) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getInt pref for key $key", e); defValue }
        override fun getLong(key: String?, defValue: Long): Long = try { delegate.getLong(key, defValue) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getLong pref for key $key", e); defValue }
        override fun getFloat(key: String?, defValue: Float): Float = try { delegate.getFloat(key, defValue) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getFloat pref for key $key", e); defValue }
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = try { delegate.getBoolean(key, defValue) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to getBoolean pref for key $key", e); defValue }
        override fun contains(key: String?): Boolean = try { delegate.contains(key) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to check contains for key $key", e); false }
        override fun edit(): SharedPreferences.Editor = try { SafeEditor(delegate.edit()) } catch (e: Throwable) { Log.e("Shikkhaloy", "Failed to edit prefs", e); context.getSharedPreferences("shikkhaloy_prefs_fallback", Context.MODE_PRIVATE).edit() }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { try { delegate.registerOnSharedPreferenceChangeListener(listener) } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to register preference listener", e) } }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { try { delegate.unregisterOnSharedPreferenceChangeListener(listener) } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to unregister preference listener", e) } }

        private class SafeEditor(private val editor: SharedPreferences.Editor) : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor = try { editor.putString(key, value); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to putString for key $key", e); this }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = try { editor.putStringSet(key, values); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to putStringSet for key $key", e); this }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = try { editor.putInt(key, value); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to putInt for key $key", e); this }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = try { editor.putLong(key, value); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to putLong for key $key", e); this }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = try { editor.putFloat(key, value); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to putFloat for key $key", e); this }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = try { editor.putBoolean(key, value); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to putBoolean for key $key", e); this }
            override fun remove(key: String?): SharedPreferences.Editor = try { editor.remove(key); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to remove key $key", e); this }
            override fun clear(): SharedPreferences.Editor = try { editor.clear(); this } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to clear editor", e); this }
            override fun commit(): Boolean = try { editor.commit() } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to commit editor", e); false }
            override fun apply() { try { editor.apply() } catch(e: Throwable){ Log.e("Shikkhaloy", "Failed to apply editor", e) } }
        }
    }

    private class RoleSanitizedSharedPreferences(private val delegate: SharedPreferences) : SharedPreferences by delegate {
        override fun getAll(): MutableMap<String, *> {
            return try {
                val allMap = delegate.all
                if (allMap != null) {
                    val sanitized = allMap.toMutableMap()
                    if (sanitized["role"] == "admin") {
                        sanitized["role"] = "student"
                    }
                    sanitized
                } else {
                    mutableMapOf<String, Any>()
                }
            } catch (e: Throwable) {
                Log.e("Shikkhaloy", "Failed to getAll in RoleSanitizedSharedPreferences", e)
                mutableMapOf<String, Any>()
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return try {
                if (key == "role") {
                    val rawValue = delegate.getString(key, defValue)
                    if (rawValue == "admin") "student" else rawValue
                } else {
                    delegate.getString(key, defValue)
                }
            } catch (e: Throwable) {
                Log.e("Shikkhaloy", "Failed to getString in RoleSanitizedSharedPreferences for key $key", e)
                defValue
            }
        }
    }
}
