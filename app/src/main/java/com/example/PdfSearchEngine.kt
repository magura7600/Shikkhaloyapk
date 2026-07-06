package com.example

import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SearchResultRect(
    val pageIndex: Int,
    val x: Float, // PDF space
    val y: Float, // PDF space
    val width: Float, // PDF space
    val height: Float, // PDF space
    val text: String
)

class PDFSearchStripper(
    private val query: String,
    private val caseSensitive: Boolean,
    private val pageIdx: Int
) : PDFTextStripper() {
    val results = mutableListOf<SearchResultRect>()

    init {
        startPage = pageIdx + 1
        endPage = pageIdx + 1
        sortByPosition = true
    }

    override fun writeString(string: String?, textPositions: MutableList<TextPosition>?) {
        if (string == null || textPositions == null || string.isEmpty() || query.isEmpty()) return

        val textToSearch = if (caseSensitive) string else string.lowercase()
        val queryToSearch = if (caseSensitive) query else query.lowercase()

        var index = textToSearch.indexOf(queryToSearch)
        while (index != -1) {
            val matchPositions = mutableListOf<TextPosition>()
            for (i in index until (index + query.length)) {
                if (i < textPositions.size) {
                    matchPositions.add(textPositions[i])
                }
            }

            if (matchPositions.isNotEmpty()) {
                val minX = matchPositions.minOf { it.xDirAdj }
                val maxX = matchPositions.maxOf { it.xDirAdj + it.widthDirAdj }
                val minY = matchPositions.minOf { it.yDirAdj - it.heightDir }
                val maxY = matchPositions.maxOf { it.yDirAdj }

                results.add(
                    SearchResultRect(
                        pageIndex = pageIdx,
                        x = minX,
                        y = minY,
                        width = (maxX - minX).coerceAtLeast(1f),
                        height = (maxY - minY).coerceAtLeast(1f),
                        text = string.substring(index, (index + query.length).coerceAtMost(string.length))
                    )
                )
            }

            index = textToSearch.indexOf(queryToSearch, index + 1)
        }
    }
}

object PdfSearchEngine {
    
    suspend fun searchPdf(
        context: Context,
        file: File,
        query: String,
        caseSensitive: Boolean,
        onProgress: (Int, Int) -> Unit // searched, total
    ): List<SearchResultRect> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()
        
        val allResults = mutableListOf<SearchResultRect>()
        var doc: PDDocument? = null
        try {
            // Initialize PDFBox resource loader
            PDFBoxResourceLoader.init(context)
            
            doc = PDDocument.load(file)
            val totalPages = doc.numberOfPages
            
            for (pageIdx in 0 until totalPages) {
                val stripper = PDFSearchStripper(query, caseSensitive, pageIdx)
                // This triggers writeString for the specified page
                stripper.getText(doc)
                allResults.addAll(stripper.results)
                
                onProgress(pageIdx + 1, totalPages)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                doc?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext allResults
    }
}
