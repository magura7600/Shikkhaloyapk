# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- ENABLE OBFUSCATION AND OPTIMIZATION (STEP 4) ---
# We enable shrinking, optimization, and obfuscation.
# -dontoptimize
# -dontobfuscate

# Keep line numbers for easier crash debugging
-keepattributes SourceFile,LineNumberTable

# --- Keep critical attributes ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Metadata

# --- Our own App Code Keep Rules (CRITICAL) ---
-keep class com.example.** { *; }
-keep interface com.example.** { *; }

# Keep YoutubeDL library classes and native interfaces
-keep class com.yausername.youtubedl_android.** { *; }
-keep interface com.yausername.youtubedl_android.** { *; }
-keep class * implements com.yausername.youtubedl_android.** { *; }

# --- Kotlinx Serialization ---
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}
-keepclassmembers class * {
    *** Companion;
    *** serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

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

# --- ExoPlayer / Media3 (CRITICAL for Video Player) ---
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Firebase & Play Services ---
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- Coil ---
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# --- Android / Jetpack Compose / Reflection ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
