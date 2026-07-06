package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoLink(
    val quality: String,
    val url: String,
    val isHd: Boolean = false,
    val hasAudio: Boolean = true,
    val height: Int = 0
)

data class VideoOptions(
    val title: String,
    val description: String,
    val links: List<VideoLink>,
    val adaptiveUrl: String? = null,
    val audioUrl: String? = null
)

object FacebookVideoExtractor {

    private fun extractResolutionFromText(text: String): String? {
        val resolutions = listOf("2160", "1440", "1080", "720", "480", "360", "240", "144")
        for (res in resolutions) {
            val regex = Regex("\\b$res\\b")
            if (regex.containsMatchIn(text) || text.contains("-$res") || text.contains("_$res") || text.contains("$res-") || text.contains("${res}_") || text.contains("${res}p") || text.contains("${res}P")) {
                return "${res}p"
            }
        }
        return null
    }

    private fun shouldResolveRedirect(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("fb.watch") || 
               lower.contains("facebook.com/share") || 
               lower.contains("fb.me") || 
               (lower.contains("fb.com") && !lower.contains("facebook.com/reel") && !lower.contains("facebook.com/watch") && !lower.contains("facebook.com/videos"))
    }

    private fun resolveRedirect(urlStr: String): String {
        val trimmed = urlStr.trim()
        if (!shouldResolveRedirect(trimmed)) {
            return trimmed
        }
        try {
            var currentUrl = trimmed
            for (i in 0..4) {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val loc = connection.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        val nextUrl = if (loc.startsWith("/")) {
                            val originalUrl = java.net.URL(currentUrl)
                            "${originalUrl.protocol}://${originalUrl.host}$loc"
                        } else {
                            loc
                        }
                        
                        if (nextUrl.contains("login") || nextUrl.contains("checkpoint") || nextUrl.contains("cookie")) {
                            connection.disconnect()
                            break
                        }
                        currentUrl = nextUrl
                    } else {
                        connection.disconnect()
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
                connection.disconnect()
            }
            return currentUrl
        } catch (e: Exception) {
            return trimmed
        }
    }

    private fun resolveVideoDirectUrlRedirect(urlStr: String): String {
        try {
            var currentUrl = urlStr.trim()
            for (i in 0..4) {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestMethod("GET")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                connection.setRequestProperty("Referer", "https://www.facebook.com/")
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val loc = connection.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        currentUrl = if (loc.startsWith("/")) {
                            val originalUrl = java.net.URL(currentUrl)
                            "${originalUrl.protocol}://${originalUrl.host}$loc"
                        } else {
                            loc
                        }
                    } else {
                        connection.disconnect()
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
                connection.disconnect()
            }
            return currentUrl
        } catch (e: Exception) {
            return urlStr
        }
    }

    private fun unescapeJson(input: String): String {
        val builder = java.lang.StringBuilder()
        var i = 0
        val len = input.length
        while (i < len) {
            val c = input[i]
            if (c == '\\' && i + 1 < len) {
                val next = input[i + 1]
                if (next == 'u' && i + 5 < len) {
                    try {
                        val hex = input.substring(i + 2, i + 6)
                        val value = hex.toInt(16).toChar()
                        builder.append(value)
                        i += 6
                    } catch (e: Exception) {
                        builder.append(c)
                        i++
                    }
                } else if (next == '/') {
                    builder.append('/')
                    i += 2
                } else if (next == 'n') {
                    builder.append('\n')
                    i += 2
                } else if (next == 't') {
                    builder.append('\t')
                    i += 2
                } else if (next == 'r') {
                    builder.append('\r')
                    i += 2
                } else if (next == '"' || next == '\'' || next == '\\') {
                    builder.append(next)
                    i += 2
                } else {
                    builder.append(c)
                    i++
                }
            } else {
                builder.append(c)
                i++
            }
        }
        return builder.toString()
    }

    private fun tryDirectHtmlExtraction(fbUrl: String): VideoOptions? {
        try {
            var cleanUrlStr = fbUrl.trim()
            if (cleanUrlStr.isNotBlank() && !cleanUrlStr.startsWith("http://") && !cleanUrlStr.startsWith("https://")) {
                cleanUrlStr = "https://$cleanUrlStr"
            }
            val resolvedUrl = resolveRedirect(cleanUrlStr)
            val url = java.net.URL(resolvedUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            
            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                
                val unescapedHtml = unescapeJson(html)
                val links = mutableListOf<VideoLink>()
                
                // 1. Try explicit json property keys
                val sdVideoRegex = """"browser_native_sd_url"\s*:\s*"([^"]+)"""".toRegex()
                val hdVideoRegex = """"browser_native_hd_url"\s*:\s*"([^"]+)"""".toRegex()
                val playableRegex = """"playable_url"\s*:\s*"([^"]+)"""".toRegex()
                val playableHdRegex = """"playable_url_quality_hd"\s*:\s*"([^"]+)"""".toRegex()
                val sdSrcRegex = """"sd_src"\s*:\s*"([^"]+)"""".toRegex()
                val hdSrcRegex = """"hd_src"\s*:\s*"([^"]+)"""".toRegex()
                val hdNoRateLimitRegex = """"hd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()
                val sdNoRateLimitRegex = """"sd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()
                
                // Extract progressive streams using findAll
                hdVideoRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }
                playableHdRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }
                hdSrcRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }
                hdNoRateLimitRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }
                sdVideoRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("360p (Direct)", resolved, isHd = false, hasAudio = true, height = 360))
                    }
                }
                playableRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("360p (Direct)", resolved, isHd = false, hasAudio = true, height = 360))
                    }
                }
                sdSrcRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("360p (Direct)", resolved, isHd = false, hasAudio = true, height = 360))
                    }
                }
                sdNoRateLimitRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("360p (Direct)", resolved, isHd = false, hasAudio = true, height = 360))
                    }
                }
                
                // 2. Scan for base_url and extract quality, height, width
                val urlPattern = """"base_url"\s*:\s*"([^"]+)"""".toRegex()
                val allUrlMatches = urlPattern.findAll(unescapedHtml).toList()
                
                var extractedAudioUrl: String? = null
                
                // First pass: find the best audio stream
                try {
                    for (urlMatch in allUrlMatches) {
                        val urlIndex = urlMatch.range.first
                        val searchStart = (urlIndex - 1000).coerceAtLeast(0)
                        val searchEnd = (urlIndex + 1000).coerceAtMost(unescapedHtml.length)
                        val surroundingText = unescapedHtml.substring(searchStart, searchEnd)
                        
                        val isAudio = surroundingText.contains("audio/mp4", ignoreCase = true) || 
                                     surroundingText.contains("audio/webm", ignoreCase = true) || 
                                     surroundingText.contains("\"mime_type\":\"audio", ignoreCase = true) ||
                                     (surroundingText.contains("audio", ignoreCase = true) && !surroundingText.contains("quality_label", ignoreCase = true))
                        
                        if (isAudio) {
                            val rawUrl = urlMatch.groups[1]?.value ?: ""
                            if (rawUrl.isNotBlank() && rawUrl.startsWith("http")) {
                                val cleanUrl = rawUrl.replace("&amp;", "&").replace("\\/", "/")
                                extractedAudioUrl = resolveVideoDirectUrlRedirect(cleanUrl)
                                break // Use the first suitable audio stream
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Second pass: extract all video streams (with and without quality_labels)
                try {
                    val heightPattern = """"height"\s*:\s*"?(\d+)"?""".toRegex()
                    val labelInBlockPattern = """"quality_label"\s*:\s*"([^"]+)"""".toRegex()
                    
                    for (urlMatch in allUrlMatches) {
                        val rawUrl = urlMatch.groups[1]?.value ?: ""
                        if (rawUrl.isBlank() || !rawUrl.startsWith("http")) continue
                        
                        val urlIndex = urlMatch.range.first
                        val searchStart = (urlIndex - 1200).coerceAtLeast(0)
                        val searchEnd = (urlIndex + 1200).coerceAtMost(unescapedHtml.length)
                        val surroundingText = unescapedHtml.substring(searchStart, searchEnd)
                        
                        // If it's an audio stream, skip it for video stream extraction
                        val isAudio = surroundingText.contains("audio/mp4", ignoreCase = true) || 
                                     surroundingText.contains("audio/webm", ignoreCase = true) || 
                                     surroundingText.contains("\"mime_type\":\"audio", ignoreCase = true) ||
                                     (surroundingText.contains("audio", ignoreCase = true) && !surroundingText.contains("quality_label", ignoreCase = true))
                        if (isAudio) continue
                        
                        // Extract height from the nearest height field in surroundingText
                        val centerOffset = urlIndex - searchStart
                        val heightMatches = heightPattern.findAll(surroundingText).toList()
                        val closestHeightMatch = heightMatches.minByOrNull { Math.abs(it.range.first - centerOffset) }
                        val height = closestHeightMatch?.groups?.get(1)?.value?.toIntOrNull() ?: 0
                        
                        // Extract quality_label if present
                        val labelMatches = labelInBlockPattern.findAll(surroundingText).toList()
                        val closestLabelMatch = labelMatches.minByOrNull { Math.abs(it.range.first - centerOffset) }
                        var label = closestLabelMatch?.groups?.get(1)?.value ?: ""
                        
                        // If label is missing but height is known, construct the label (e.g. 480p)
                        if (label.isBlank() && height > 0) {
                            label = "${height}p"
                        }
                        
                        if (height > 0 || label.isNotBlank()) {
                            val cleanUrl = rawUrl.replace("&amp;", "&").replace("\\/", "/")
                            val resolvedUrl = resolveVideoDirectUrlRedirect(cleanUrl)
                            val finalLabel = if (label.isNotBlank()) label else "${height}p"
                            links.add(
                                VideoLink(
                                    quality = finalLabel,
                                    url = resolvedUrl,
                                    isHd = height >= 720,
                                    hasAudio = false, // DASH video streams are video-only
                                    height = height
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
 
                // 3. Scan for og:video tags as ultimate backup
                val ogVideoRegex = """<meta\s+property="og:video:secure_url"\s+content="([^"]+)"""".toRegex()
                val ogVideoRegex2 = """<meta\s+property="og:video"\s+content="([^"]+)"""".toRegex()
                val ogVideo = ogVideoRegex.find(unescapedHtml)?.groups?.get(1)?.value
                    ?: ogVideoRegex2.find(unescapedHtml)?.groups?.get(1)?.value
                if (!ogVideo.isNullOrBlank()) {
                    val cleanOgVideo = ogVideo.replace("&amp;", "&").replace("\\/", "/")
                    val resolvedOgVideo = resolveVideoDirectUrlRedirect(cleanOgVideo)
                    val isHd = resolvedOgVideo.contains("hd") || resolvedOgVideo.contains("1080") || resolvedOgVideo.contains("720")
                    val qualityLabel = if (isHd) "720p (Direct)" else "360p (Direct)"
                    links.add(VideoLink(qualityLabel, resolvedOgVideo, isHd = isHd, hasAudio = true, height = if (isHd) 720 else 360))
                }
                
                // Filter out any empty URLs and deduplicate based on quality
                val validLinks = links.filter { it.url.isNotBlank() }
                val uniqueLinks = mutableListOf<VideoLink>()
                val seenQualities = mutableSetOf<String>()
                
                // Sort all discovered qualities by height descending (highest quality first)
                val sortedLinks = validLinks.sortedWith(compareByDescending<VideoLink> { it.height }.thenBy { it.quality })
                sortedLinks.forEach { link ->
                    if (link.quality !in seenQualities) {
                        uniqueLinks.add(link)
                        seenQualities.add(link.quality)
                    }
                }
                
                if (uniqueLinks.isNotEmpty()) {
                    return VideoOptions(
                        title = "Facebook Video",
                        description = "Web direct extracted video streams",
                        links = uniqueLinks,
                        adaptiveUrl = uniqueLinks.first().url,
                        audioUrl = extractedAudioUrl
                    )
                }
            } else {
                connection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun extractVideoOptions(context: Context, rawFbUrl: String): VideoOptions? = withContext(Dispatchers.IO) {
        var cleanRawUrl = rawFbUrl.trim()
        if (cleanRawUrl.isNotBlank() && !cleanRawUrl.startsWith("http://") && !cleanRawUrl.startsWith("https://")) {
            cleanRawUrl = "https://$cleanRawUrl"
        }
        val fbUrl = resolveRedirect(cleanRawUrl)
        
        // 1. Check if the URL is already a direct playable video link
        val lowerUrl = fbUrl.lowercase()
        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".mpd") || lowerUrl.contains(".mkv") || lowerUrl.contains(".3gp") || lowerUrl.contains(".webm")) {
            val resolvedUrl = resolveVideoDirectUrlRedirect(fbUrl)
            val isHd = resolvedUrl.contains("hd") || resolvedUrl.contains("1080") || resolvedUrl.contains("720")
            return@withContext VideoOptions(
                title = "Direct Video Stream",
                description = "Direct playable video link",
                links = listOf(
                    VideoLink(
                        quality = "Default (Direct)",
                        url = resolvedUrl,
                        isHd = isHd,
                        hasAudio = true,
                        height = if (isHd) 720 else 360
                    )
                ),
                adaptiveUrl = resolvedUrl
            )
        }

        // 2. Direct HTML extraction from the video page (fast and lightweight)
        try {
            val directHtmlOptions = tryDirectHtmlExtraction(fbUrl)
            if (directHtmlOptions != null && directHtmlOptions.links.isNotEmpty()) {
                return@withContext directHtmlOptions
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Ultimate robust fallback: if everything fails, return the original/resolved URL as default quality
        if (fbUrl.startsWith("http://") || fbUrl.startsWith("https://")) {
            val resolvedUrl = resolveVideoDirectUrlRedirect(fbUrl)
            val isHd = resolvedUrl.contains("hd") || resolvedUrl.contains("1080") || resolvedUrl.contains("720")
            return@withContext VideoOptions(
                title = "Video Stream",
                description = "Direct fallback play",
                links = listOf(
                    VideoLink(
                        quality = "Default (Fallback)",
                        url = resolvedUrl,
                        isHd = isHd,
                        hasAudio = true,
                        height = if (isHd) 720 else 360
                    )
                ),
                adaptiveUrl = resolvedUrl
            )
        }
        return@withContext null
    }
}
