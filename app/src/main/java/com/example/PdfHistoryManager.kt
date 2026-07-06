package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

@Serializable
data class FavoritePdf(
    val title: String,
    val filePath: String,
    val url: String,
    val timestamp: Long
)

@Serializable
data class RecentPdf(
    val title: String,
    val filePath: String,
    val url: String,
    val timestamp: Long
)

@Serializable
data class BookmarkRecord(
    val id: String,
    val pageIndex: Int,
    val note: String,
    val timestamp: Long
)

object PdfHistoryManager {
    private const val PREFS_NAME = "shikkhaloy_pdf_history_prefs"
    private const val KEY_LAST_POSITIONS = "last_reading_positions"
    private const val KEY_FAVORITES = "favorite_pdfs"
    private const val KEY_RECENTS = "recent_pdfs"
    private const val KEY_BOOKMARKS_PREFIX = "bookmarks_"

    private val json = Json { ignoreUnknownKeys = true }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPdfKey(file: File, url: String): String {
        return if (url.isNotBlank()) url else file.absolutePath
    }

    // --- LAST READING POSITION ---

    fun saveLastPage(context: Context, pdfKey: String, pageIndex: Int) {
        val prefs = getPrefs(context)
        val positionsJson = prefs.getString(KEY_LAST_POSITIONS, "{}") ?: "{}"
        try {
            val positions = json.decodeFromString<Map<String, Int>>(positionsJson).toMutableMap()
            positions[pdfKey] = pageIndex
            prefs.edit().putString(KEY_LAST_POSITIONS, json.encodeToString(positions)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLastPage(context: Context, pdfKey: String): Int {
        val prefs = getPrefs(context)
        val positionsJson = prefs.getString(KEY_LAST_POSITIONS, "{}") ?: "{}"
        return try {
            val positions = json.decodeFromString<Map<String, Int>>(positionsJson)
            positions[pdfKey] ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // --- FAVORITES ---

    fun isFavorite(context: Context, filePath: String, url: String): Boolean {
        val favorites = getFavorites(context)
        return favorites.any { it.filePath == filePath || (url.isNotBlank() && it.url == url) }
    }

    fun toggleFavorite(context: Context, title: String, filePath: String, url: String) {
        val prefs = getPrefs(context)
        val favorites = getFavorites(context).toMutableList()
        val existing = favorites.find { it.filePath == filePath || (url.isNotBlank() && it.url == url) }
        
        if (existing != null) {
            favorites.remove(existing)
        } else {
            favorites.add(FavoritePdf(title, filePath, url, System.currentTimeMillis()))
        }
        
        try {
            prefs.edit().putString(KEY_FAVORITES, json.encodeToString(favorites)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFavorites(context: Context): List<FavoritePdf> {
        val prefs = getPrefs(context)
        val favsJson = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<FavoritePdf>>(favsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- RECENTS ---

    fun addRecent(context: Context, title: String, filePath: String, url: String) {
        val prefs = getPrefs(context)
        val recents = getRecents(context).toMutableList()
        
        // Remove duplicate
        recents.removeAll { it.filePath == filePath || (url.isNotBlank() && it.url == url) }
        
        // Add new to front
        recents.add(0, RecentPdf(title, filePath, url, System.currentTimeMillis()))
        
        // Limit to 20 recents
        val limited = recents.take(20)
        
        try {
            prefs.edit().putString(KEY_RECENTS, json.encodeToString(limited)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecents(context: Context): List<RecentPdf> {
        val prefs = getPrefs(context)
        val recentsJson = prefs.getString(KEY_RECENTS, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<RecentPdf>>(recentsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- BOOKMARKS ---

    fun addBookmark(context: Context, pdfKey: String, pageIndex: Int, note: String) {
        val prefs = getPrefs(context)
        val key = KEY_BOOKMARKS_PREFIX + pdfKey.hashCode()
        val bookmarks = getBookmarks(context, pdfKey).toMutableList()
        
        bookmarks.add(BookmarkRecord(
            id = java.util.UUID.randomUUID().toString(),
            pageIndex = pageIndex,
            note = note.trim(),
            timestamp = System.currentTimeMillis()
        ))
        // Sort by pageIndex
        bookmarks.sortBy { it.pageIndex }
        
        try {
            prefs.edit().putString(key, json.encodeToString(bookmarks)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteBookmark(context: Context, pdfKey: String, bookmarkId: String) {
        val prefs = getPrefs(context)
        val key = KEY_BOOKMARKS_PREFIX + pdfKey.hashCode()
        val bookmarks = getBookmarks(context, pdfKey).toMutableList()
        
        bookmarks.removeAll { it.id == bookmarkId }
        
        try {
            prefs.edit().putString(key, json.encodeToString(bookmarks)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBookmarks(context: Context, pdfKey: String): List<BookmarkRecord> {
        val prefs = getPrefs(context)
        val key = KEY_BOOKMARKS_PREFIX + pdfKey.hashCode()
        val bookmarksJson = prefs.getString(key, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<BookmarkRecord>>(bookmarksJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
