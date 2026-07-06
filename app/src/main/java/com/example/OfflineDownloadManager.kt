package com.example

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

@Serializable
data class DownloadRecord(
    val id: String,
    val title: String,
    val url: String,
    val fileType: String, // "pdf" or "video"
    val localPath: String, // relative path or absolute path
    val downloadTime: Long,
    val courseName: String = "",
    val className: String = ""
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Success(val record: DownloadRecord) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object OfflineDownloadManager {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val PREFS_NAME = "shikkhaloy_downloads_prefs"
    private const val KEY_DOWNLOADS = "downloaded_records"

    // Map to track active download states by URL or ID
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates

    private fun updateDownloadState(url: String, state: DownloadState) {
        val current = _downloadStates.value.toMutableMap()
        current[url] = state
        _downloadStates.value = current
    }

    fun getDownloadRecords(context: Context): List<DownloadRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<DownloadRecord>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDownloadRecords(context: Context, records: List<DownloadRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = Json.encodeToString(records)
        prefs.edit().putString(KEY_DOWNLOADS, jsonStr).apply()
    }

    // Download a file permanently to app's secure internal storage
    fun downloadPermanently(
        context: Context,
        url: String,
        title: String,
        fileType: String, // "pdf" or "video"
        courseName: String = "",
        className: String = ""
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        updateDownloadState(cleanUrl, DownloadState.Downloading(0f))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Determine file extension
                val ext = if (fileType == "pdf") "pdf" else "mp4"
                
                // Create secure downloads directory inside internal filesDir
                val secureDir = File(context.filesDir, "secure_downloads")
                if (!secureDir.exists()) {
                    secureDir.mkdirs()
                }

                val uniqueName = "dl_${System.currentTimeMillis()}_${title.hashCode()}.$ext"
                val destinationFile = File(secureDir, uniqueName)

                val sanitizedUrl = sanitizeUrl(cleanUrl)
                val request = Request.Builder().url(sanitizedUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download file: HTTP code ${response.code}")
                    }
                    val body = response.body ?: throw Exception("Response body is null")
                    val contentLength = body.contentLength()
                    
                    body.byteStream().use { inputStream ->
                        FileOutputStream(destinationFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (contentLength > 0) {
                                    val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                    updateDownloadState(cleanUrl, DownloadState.Downloading(progress))
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    // Create and save download record
                    val record = DownloadRecord(
                        id = cleanUrl.hashCode().toString(),
                        title = title,
                        url = cleanUrl,
                        fileType = fileType,
                        localPath = destinationFile.absolutePath,
                        downloadTime = System.currentTimeMillis(),
                        courseName = courseName,
                        className = className
                    )

                    val currentRecords = getDownloadRecords(context).toMutableList()
                    // Remove existing download of same URL if any
                    currentRecords.removeAll { it.url == cleanUrl }
                    currentRecords.add(record)
                    saveDownloadRecords(context, currentRecords)

                    withContext(Dispatchers.Main) {
                        updateDownloadState(cleanUrl, DownloadState.Success(record))
                        android.widget.Toast.makeText(context, "$title ডাউনলোড সম্পন্ন হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    updateDownloadState(cleanUrl, DownloadState.Error(e.message ?: "Unknown error"))
                    android.widget.Toast.makeText(context, "$title ডাউনলোড ব্যর্থ হয়েছে: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Download temporarily to the cache directory for direct viewing
    fun downloadToCache(
        context: Context,
        url: String,
        title: String,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            onError("URL is empty")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure cache dir exists
                val tempDir = File(context.cacheDir, "temp_pdfs")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                val tempFile = File(tempDir, "temp_${System.currentTimeMillis()}_view.pdf")
                val sanitizedUrl = sanitizeUrl(cleanUrl)
                val request = Request.Builder().url(sanitizedUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to fetch file: HTTP ${response.code}")
                    }
                    val body = response.body ?: throw Exception("Body is null")
                    
                    body.byteStream().use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            outputStream.flush()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onComplete(tempFile)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to download temporary file")
                }
            }
        }
    }

    // Delete a downloaded file permanently
    fun deleteDownload(context: Context, record: DownloadRecord) {
        val file = File(record.localPath)
        if (file.exists()) {
            file.delete()
        }
        val records = getDownloadRecords(context).toMutableList()
        records.removeAll { it.id == record.id }
        saveDownloadRecords(context, records)
        
        // Remove from states tracking
        val currentStates = _downloadStates.value.toMutableMap()
        currentStates.remove(record.url)
        _downloadStates.value = currentStates
    }

    // Clear all temporary files in cacheDir (for temporary PDF views)
    fun clearTemporaryCache(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tempDir = File(context.cacheDir, "temp_pdfs")
                if (tempDir.exists() && tempDir.isDirectory) {
                    tempDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Robustly decodes and encodes URL parts (e.g. spaces and Bengali characters)
 * so that OkHttp doesn't fail with IllegalArgumentException on unencoded URLs.
 */
fun sanitizeUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return trimmed
    
    // 1. Try OkHttp HttpUrl parse directly (safest/fastest path if already perfectly formatted)
    try {
        trimmed.toHttpUrl()
        return trimmed
    } catch (e: Exception) {
        // Fall through to manual encoding
    }

    // 2. Parse using Android's Uri which is extremely forgiving of raw spaces and unicode characters
    return try {
        val uri = android.net.Uri.parse(trimmed)
        val scheme = uri.scheme ?: "https"
        val host = uri.host ?: ""
        val port = if (uri.port != -1) ":${uri.port}" else ""
        val authority = if (host.isNotEmpty()) "$host$port" else uri.authority ?: ""
        
        // Encode each path segment correctly (getPathSegments() returns decoded segments)
        val pathSegments = uri.pathSegments
        val encodedPath = if (pathSegments.isNotEmpty()) {
            "/" + pathSegments.joinToString("/") { segment ->
                android.net.Uri.encode(segment)
            }
        } else {
            ""
        }
        
        // Encode each query parameter correctly
        val query = uri.query
        val encodedQuery = if (!query.isNullOrBlank()) {
            "?" + uri.queryParameterNames.joinToString("&") { name ->
                val value = uri.getQueryParameter(name)
                if (value != null) {
                    "${android.net.Uri.encode(name)}=${android.net.Uri.encode(value)}"
                } else {
                    android.net.Uri.encode(name)
                }
            }
        } else {
            ""
        }
        
        // Encode fragment
        val fragment = uri.fragment
        val encodedFragment = if (!fragment.isNullOrBlank()) {
            "#" + android.net.Uri.encode(fragment)
        } else {
            ""
        }
        
        val rebuilt = "$scheme://$authority$encodedPath$encodedQuery$encodedFragment"
        // Validate rebuilt URL with OkHttp
        rebuilt.toHttpUrl()
        rebuilt
    } catch (e: Exception) {
        e.printStackTrace()
        // If everything fails, replace raw spaces with %20 as a fallback
        trimmed.replace(" ", "%20")
    }
}
