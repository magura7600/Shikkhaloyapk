package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class PointData(val x: Float, val y: Float)

@Serializable
data class PdfAnnotation(
    val id: String,
    val type: String, // "highlight", "underline", "strikethrough", "draw", "rectangle", "circle", "line", "arrow", "text", "sticky"
    val pageIndex: Int,
    val color: Int, // ARGB color
    val strokeWidth: Float = 4f,
    val points: List<PointData> = emptyList(),
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val text: String = "",
    val fontSize: Float = 14f,
    val timestamp: Long = System.currentTimeMillis()
)

object PdfAnnotationManager {
    private const val PREFS_NAME = "shikkhaloy_pdf_annotations_prefs"
    private const val KEY_ANNOTATIONS_PREFIX = "annotations_"
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveAnnotations(context: Context, pdfKey: String, annotations: List<PdfAnnotation>) {
        val prefs = getPrefs(context)
        val key = KEY_ANNOTATIONS_PREFIX + pdfKey.hashCode()
        try {
            val data = json.encodeToString(annotations)
            prefs.edit().putString(key, data).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getAnnotations(context: Context, pdfKey: String): List<PdfAnnotation> {
        val prefs = getPrefs(context)
        val key = KEY_ANNOTATIONS_PREFIX + pdfKey.hashCode()
        val data = prefs.getString(key, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<PdfAnnotation>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
