import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  // alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.kotlin.serialization)
}

// Automatically clean up legacy mipmap folders and duplicate resources to prevent compilation failures on remote build systems (like GitHub Actions)
val resDir = file("src/main/res")
if (resDir.exists()) {
  // 1. Delete legacy mipmaps (mipmap-hdpi, mipmap-mdpi, etc.)
  resDir.listFiles()?.forEach { dir ->
    if (dir.isDirectory && dir.name.startsWith("mipmap-") && !dir.name.contains("anydpi")) {
      println("Automatically deleting legacy mipmap folder at configuration time: ${dir.absolutePath}")
      dir.deleteRecursively()
    }
  }
  // 2. Delete custom_logo.webp from drawable to prevent duplicate resource conflicts with custom_logo.png
  val duplicateLogoWebp = file("src/main/res/drawable/custom_logo.webp")
  if (duplicateLogoWebp.exists()) {
    println("Automatically deleting duplicate logo webp: ${duplicateLogoWebp.absolutePath}")
    duplicateLogoWebp.delete()
  }
}

android {
  androidResources {
    ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
  }
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.shikkhaloyai.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 12
    versionName = "12.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      if (file(keystorePath).exists()) {
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        // Fallback to debug keystore to allow building unsigned/debug-signed release APKs
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Use default debug signing config automatically
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  packaging {
    jniLibs.useLegacyPackaging = true
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  
  // YouTube/Video related - check if they are actually used
  // implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
  
  // Google Sign-In with Credential Manager (Unused, commented out to save size)
  // implementation("androidx.credentials:credentials:1.3.0-rc01")
  // implementation("androidx.credentials:credentials-play-services-auth:1.3.0-rc01")
  // implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  implementation("androidx.browser:browser:1.8.0")
  
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.okhttp)
  
  // PDFBox for Android to search text and get precise coordinates (Unused, commented out to save space)
  // implementation("com.tom-roush:pdfbox-android:2.0.27.0")
  
  // Supabase
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.auth)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.kotlinx.serialization.json)
  
  // OneSignal - used?
  implementation(libs.onesignal)
  
  // Firebase Auth
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-auth")
  
  // implementation(libs.retrofit)
  
  // ExoPlayer
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.exoplayer.dash)
  implementation(libs.media3.exoplayer.hls)
  implementation(libs.media3.ui)

  // App Signature & Play Integrity Management (Unused, commented out to save space)
  // implementation("com.google.android.play:integrity:1.3.0")
  // implementation("com.google.android.gms:play-services-auth-api-phone:18.0.2")

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
