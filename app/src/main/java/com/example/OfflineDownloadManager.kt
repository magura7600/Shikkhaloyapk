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
                val request = Request.Builder().url(sanitizedUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").build()
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
                val request = Request.Builder().url(sanitizedUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").build()
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
    try {
        trimmed.toHttpUrl()
        return trimmed
    } catch (e: Exception) {
        // Continue to custom encoder
    }

    try {
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd == -1) return trimmed
        val scheme = trimmed.substring(0, schemeEnd + 3)
        val rest = trimmed.substring(schemeEnd + 3)
        
        val firstSlash = rest.indexOf('/')
        if (firstSlash == -1) return trimmed
        
        val host = rest.substring(0, firstSlash)
        val pathAndQuery = rest.substring(firstSlash)
        
        val queryStart = pathAndQuery.indexOf('?')
        val path = if (queryStart == -1) pathAndQuery else pathAndQuery.substring(0, queryStart)
        val query = if (queryStart == -1) "" else pathAndQuery.substring(queryStart)
        
        val encodedPath = path.split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) "" else {
                java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
        }
        
        return "$scheme$host$encodedPath$query"
    } catch (e: Exception) {
        e.printStackTrace()
        return trimmed
    }
}
