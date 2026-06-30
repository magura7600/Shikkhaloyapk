package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Serializable
data class AppUpdate(
    val id: Int? = null,
    val version_code: Int,
    val version_name: String,
    val apk_url: String,
    val changelog: String = "",
    val is_force_update: Boolean = false,
    val created_at: String? = null
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    class Downloading(val progress: Float) : UpdateDownloadState()
    class Success(val apkFile: File) : UpdateDownloadState()
    class Error(val message: String) : UpdateDownloadState()
}

object AppUpdateManager {

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState

    private val client = OkHttpClient()

    /**
     * Gets the current version code of the running app.
     */
    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Gets the current version name of the running app.
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Checks Supabase for any new updates.
     * Returns the [AppUpdate] if a newer version is available, or null otherwise.
     */
    suspend fun checkForUpdate(context: Context): AppUpdate? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch all update records from Supabase
                val updates = supabase.from("app_updates").select().decodeList<AppUpdate>()
                val latestUpdate = updates.maxByOrNull { it.version_code }
                
                if (latestUpdate != null) {
                    val currentCode = getCurrentVersionCode(context)
                    if (latestUpdate.version_code > currentCode) {
                        return@withContext latestUpdate
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                // Return null if table does not exist or network fails
                null
            }
        }
    }

    /**
     * Publishes a new update to Supabase.
     */
    suspend fun publishUpdate(update: AppUpdate): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("app_updates").insert(update)
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Downloads the APK file from the given url to cache.
     */
    suspend fun downloadApk(context: Context, url: String) {
        withContext(Dispatchers.IO) {
            _downloadState.value = UpdateDownloadState.Downloading(0f)
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw Exception("Failed to download: HTTP ${response.code}")
                }
                
                val body = response.body ?: throw Exception("Response body is empty")
                val totalBytes = body.contentLength()
                val apkFile = File(context.cacheDir, "app_update_latest.apk")
                
                // Delete existing if any
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                body.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                _downloadState.value = UpdateDownloadState.Downloading(progress)
                            }
                        }
                    }
                }
                
                _downloadState.value = UpdateDownloadState.Success(apkFile)
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadState.value = UpdateDownloadState.Error(e.message ?: "ডাউনলোড করতে সমস্যা হয়েছে")
            }
        }
    }

    /**
     * Triggers the package installer to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        // On API 26+, check if the app can install other apps. If not, open Settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Resets download state to Idle
     */
    fun resetState() {
        _downloadState.value = UpdateDownloadState.Idle
    }
}
