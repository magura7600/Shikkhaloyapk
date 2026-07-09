# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Our own App Code Keep Rules (CRITICAL) ---
# Keeping everything in com.example completely avoids crashes related to serialization, reflection, and database models.
-keep class com.example.** { *; }
-keep interface com.example.** { *; }

# Keep YoutubeDL library classes and native interfaces
-keep class com.yausername.youtubedl_android.** { *; }
-keep interface com.yausername.youtubedl_android.** { *; }
-keep class * implements com.yausername.youtubedl_android.** { *; }

# --- Kotlinx Serialization ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** $serializer;
}
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}

# --- Supabase & Ktor & Kotlin Coroutines ---
-keep class io.github.jan.supabase.** { *; }
-keep interface io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# --- OkHttp & Okio ---
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- OneSignal ---
-keep class com.onesignal.** { *; }
-keep interface com.onesignal.** { *; }
-dontwarn com.onesignal.**

# --- Android / Jetpack Compose / Reflection ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepattributes Signature,InnerClasses,EnclosingMethod,Annotation

