package com.example

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@Serializable
data class ImgBBResponse(
    val data: ImgBBData? = null,
    val success: Boolean = false,
    val status: Int = 0
)

@Serializable
data class ImgBBData(
    val id: String = "",
    val url: String = "",
    val display_url: String = "",
    val delete_url: String? = null
)

object ImgBBClient {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun uploadImage(imageBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image", 
                    "upload.jpg", 
                    imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=${BuildConfig.IMGBB_API_KEY}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val imgBBResponse = json.decodeFromString<ImgBBResponse>(responseBody)
                    if (imgBBResponse.success) {
                        return@withContext imgBBResponse.data?.url
                    }
                }
                Log.e("ImgBBClient", "Upload failed: $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e("ImgBBClient", "Exception during upload", e)
            null
        }
    }
}
