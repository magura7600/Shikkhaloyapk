package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class RoleSanitizerTest {

    @Test
    fun testRoleSanitizer_sanitizesAdminToStudent_forGetString() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawPrefs = context.getSharedPreferences("test_raw_prefs", Context.MODE_PRIVATE)
        rawPrefs.edit().putString("role", "admin").commit()

        // Act
        // Accessing via the custom PrefUtils fallback RoleSanitizedSharedPreferences class
        // To test it, we find the private constructor or we can mock/instantiate it via reflection or direct testing
        // Wait, is there a direct way to instantiate RoleSanitizedSharedPreferences?
        // Since RoleSanitizedSharedPreferences is private in PrefUtils, let's get the secure prefs using getSecurePrefs
        // but we want to force the fallback. Since we cannot easily fail the MasterKey builder on JVM/Robolectric by default,
        // we can use reflection to construct RoleSanitizedSharedPreferences or test via getSecurePrefs if we mock/intercept.
        // Actually, let's find the inner class using reflection to test it directly!
        val nestedClasses = PrefUtils::class.java.declaredClasses
        val roleSanitizedClass = nestedClasses.firstOrNull { it.simpleName == "RoleSanitizedSharedPreferences" }
        
        if (roleSanitizedClass != null) {
            val constructor = roleSanitizedClass.getDeclaredConstructor(android.content.SharedPreferences::class.java)
            constructor.isAccessible = true
            val sanitizedPrefs = constructor.newInstance(rawPrefs) as android.content.SharedPreferences

            // Assert - role of "admin" must be sanitized to "student"
            assertEquals("student", sanitizedPrefs.getString("role", null))

            // Assert - other roles are NOT sanitized
            rawPrefs.edit().putString("role", "teacher").commit()
            assertEquals("teacher", sanitizedPrefs.getString("role", null))

            rawPrefs.edit().putString("role", "student").commit()
            assertEquals("student", sanitizedPrefs.getString("role", null))
        } else {
            throw AssertionError("RoleSanitizedSharedPreferences class not found in PrefUtils")
        }
    }

    @Test
    fun testRoleSanitizer_sanitizesAdminToStudent_forGetAll() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawPrefs = context.getSharedPreferences("test_raw_prefs_all", Context.MODE_PRIVATE)
        rawPrefs.edit().putString("role", "admin").putString("other_key", "value").commit()

        // Act
        val nestedClasses = PrefUtils::class.java.declaredClasses
        val roleSanitizedClass = nestedClasses.firstOrNull { it.simpleName == "RoleSanitizedSharedPreferences" }

        if (roleSanitizedClass != null) {
            val constructor = roleSanitizedClass.getDeclaredConstructor(android.content.SharedPreferences::class.java)
            constructor.isAccessible = true
            val sanitizedPrefs = constructor.newInstance(rawPrefs) as android.content.SharedPreferences

            // Assert - getAll() must return "student" instead of "admin" for "role"
            val allPrefs = sanitizedPrefs.all
            assertEquals("student", allPrefs["role"])
            assertEquals("value", allPrefs["other_key"])
        } else {
            throw AssertionError("RoleSanitizedSharedPreferences class not found in PrefUtils")
        }
    }
}
