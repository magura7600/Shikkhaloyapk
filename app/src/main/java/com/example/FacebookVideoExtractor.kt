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
                
                var sdUrl = sdVideoRegex.find(unescapedHtml)?.groups?.get(1)?.value
                    ?: sdSrcRegex.find(unescapedHtml)?.groups?.get(1)?.value
                var hdUrl = hdVideoRegex.find(unescapedHtml)?.groups?.get(1)?.value
                    ?: hdSrcRegex.find(unescapedHtml)?.groups?.get(1)?.value
                var playable = playableRegex.find(unescapedHtml)?.groups?.get(1)?.value
                var playableHd = playableHdRegex.find(unescapedHtml)?.groups?.get(1)?.value
                
                if (!hdUrl.isNullOrBlank()) {
                    val cleanHd = hdUrl.replace("&amp;", "&").replace("\\/", "/")
                    val resolvedHd = resolveVideoDirectUrlRedirect(cleanHd)
                    links.add(VideoLink("720p", resolvedHd, isHd = true, hasAudio = true, height = 720))
                }
                if (!playableHd.isNullOrBlank()) {
                    val cleanPlayableHd = playableHd.replace("&amp;", "&").replace("\\/", "/")
                    val resolvedPlayableHd = resolveVideoDirectUrlRedirect(cleanPlayableHd)
                    links.add(VideoLink("720p", resolvedPlayableHd, isHd = true, hasAudio = true, height = 720))
                }
                if (!sdUrl.isNullOrBlank()) {
                    val cleanSd = sdUrl.replace("&amp;", "&").replace("\\/", "/")
                    val resolvedSd = resolveVideoDirectUrlRedirect(cleanSd)
                    links.add(VideoLink("360p", resolvedSd, isHd = false, hasAudio = true, height = 360))
                }
                if (!playable.isNullOrBlank()) {
                    val cleanPlayable = playable.replace("&amp;", "&").replace("\\/", "/")
                    val resolvedPlayable = resolveVideoDirectUrlRedirect(cleanPlayable)
                    links.add(VideoLink("360p", resolvedPlayable, isHd = false, hasAudio = true, height = 360))
                }
                
                // 2. Scan for og:video tags as ultimate backup
                val ogVideoRegex = """<meta\s+property="og:video:secure_url"\s+content="([^"]+)"""".toRegex()
                val ogVideoRegex2 = """<meta\s+property="og:video"\s+content="([^"]+)"""".toRegex()
                var ogVideo = ogVideoRegex.find(unescapedHtml)?.groups?.get(1)?.value
                    ?: ogVideoRegex2.find(unescapedHtml)?.groups?.get(1)?.value
                if (!ogVideo.isNullOrBlank()) {
                    val cleanOgVideo = ogVideo.replace("&amp;", "&").replace("\\/", "/")
                    val resolvedOgVideo = resolveVideoDirectUrlRedirect(cleanOgVideo)
                    val isHd = resolvedOgVideo.contains("hd") || resolvedOgVideo.contains("1080") || resolvedOgVideo.contains("720")
                    val qualityLabel = if (isHd) "720p" else "360p"
                    links.add(VideoLink(qualityLabel, resolvedOgVideo, isHd = isHd, hasAudio = true, height = if (isHd) 720 else 360))
                }
                
                // Filter out any empty URLs and deduplicate to ensure unique "720p" and/or "360p" choices
                val validLinks = links.filter { it.url.isNotBlank() }
                val uniqueLinks = mutableListOf<VideoLink>()
                val seenQualities = mutableSetOf<String>()
                
                // Prefer 720p first, then 360p
                validLinks.firstOrNull { it.quality == "720p" }?.let {
                    uniqueLinks.add(it)
                    seenQualities.add("720p")
                }
                validLinks.firstOrNull { it.quality == "360p" }?.let {
                    uniqueLinks.add(it)
                    seenQualities.add("360p")
                }
                
                // Fallback for any other custom qualities
                validLinks.forEach { link ->
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
                        adaptiveUrl = uniqueLinks.first().url
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
