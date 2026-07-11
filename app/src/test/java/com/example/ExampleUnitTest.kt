package com.example

import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ExampleUnitTest {
    
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

    @Test
    fun testShouldResolveRedirect() {
        assert(shouldResolveRedirect("https://fb.watch/123"))
        assert(shouldResolveRedirect("https://facebook.com/share/v/123"))
        assert(shouldResolveRedirect("https://fb.me/xyz"))
        assert(!shouldResolveRedirect("https://facebook.com/reel/123"))
        assert(!shouldResolveRedirect("https://facebook.com/watch/?v=123"))
    }

    @Test
    fun testResolveRedirect_returnsOriginalUrlIfNoRedirectNeeded() {
        val originalUrl = "https://facebook.com/reel/123"
        val resolved = resolveRedirect(originalUrl)
        org.junit.Assert.assertEquals(originalUrl, resolved)
    }
}
