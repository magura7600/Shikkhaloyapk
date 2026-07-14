package com.example

object YouTubeVideoExtractor {
    fun isYouTubeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    fun extractVideoId(url: String): String? {
        if (!isYouTubeUrl(url)) return null
        
        val regex = listOf(
            "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*",
            "(?<=/live/)[^#\\&\\?\\n]*",
            "(?<=/shorts/)[^#\\&\\?\\n]*"
        )
        
        for (pattern in regex) {
            val compiledPattern = java.util.regex.Pattern.compile(pattern)
            val matcher = compiledPattern.matcher(url)
            if (matcher.find()) {
                return matcher.group()
            }
        }
        
        return null
    }
}
