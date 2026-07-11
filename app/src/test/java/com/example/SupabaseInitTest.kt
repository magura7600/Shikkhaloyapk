package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.CodeVerifierCache

class MyCodeVerifierCache(context: Context) : CodeVerifierCache {
    private val prefs = context.getSharedPreferences("code_verifier", Context.MODE_PRIVATE)
    override suspend fun saveCodeVerifier(codeVerifier: String) {
        prefs.edit().putString("code", codeVerifier).apply()
    }
    override suspend fun loadCodeVerifier(): String? = prefs.getString("code", null)
    override suspend fun deleteCodeVerifier() {
        prefs.edit().remove("code").apply()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class SupabaseInitTest {
    @Test
    fun testInit() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        createSupabaseClient("https://gputlynskpbbiexbpphj.supabase.co", "key") {
            install(Auth) {
                sessionManager = SharedPreferencesSessionManager(ctx)
                // Try setting codeVerifierCache
                // If it fails to compile, it means property doesn't exist
                try {
                    val authConfigClass = this::class.java
                    val method = authConfigClass.getMethod("setCodeVerifierCache", CodeVerifierCache::class.java)
                    method.invoke(this, MyCodeVerifierCache(ctx))
                } catch(e: Exception) {
                    // Try direct assignment if it compiles, but we use reflection to bypass compilation check in bash for now
                }
            }
        }
    }
}
