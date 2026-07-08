import re

with open('app/src/main/java/com/example/FacebookVideoExtractor.kt', 'r') as f:
    content = f.read()

target_vars = """                val sdSrcRegex = """"sd_src"\s*:\s*"([^"]+)"""".toRegex()
                val hdSrcRegex = """"hd_src"\s*:\s*"([^"]+)"""".toRegex()
                val hdNoRateLimitRegex = """"hd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()
                val sdNoRateLimitRegex = """"sd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()"""

replacement_vars = """                val sdSrcRegex = """"sd_src"\s*:\s*"([^"]+)"""".toRegex()
                val hdSrcRegex = """"hd_src"\s*:\s*"([^"]+)"""".toRegex()
                val hdNoRateLimitRegex = """"hd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()
                val sdNoRateLimitRegex = """"sd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()
                val hd1080Regex = """"1080_src"\s*:\s*"([^"]+)"""".toRegex()
                val hd1080NoRateLimitRegex = """"1080_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()
                val hqRegex = """"hq_src"\s*:\s*"([^"]+)"""".toRegex()"""

content = content.replace(target_vars, replacement_vars)

target_exec = """                playableHdRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }"""

replacement_exec = """                playableHdRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }
                hd1080Regex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("1080p (Direct)", resolved, isHd = true, hasAudio = true, height = 1080))
                    }
                }
                hd1080NoRateLimitRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("1080p (Direct)", resolved, isHd = true, hasAudio = true, height = 1080))
                    }
                }
                hqRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }"""

content = content.replace(target_exec, replacement_exec)

with open('app/src/main/java/com/example/FacebookVideoExtractor.kt', 'w') as f:
    f.write(content)
