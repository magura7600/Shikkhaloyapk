with open('app/src/main/java/com/example/FacebookVideoExtractor.kt', 'r') as f:
    content = f.read()

target1 = 'val sdNoRateLimitRegex = """"sd_src_no_ratelimit"\s*:\s*"([^"]+)"""".toRegex()'
rep1 = target1 + '\n                val hd1080Regex = """"1080_src"\\s*:\\s*"([^"]+)"""".toRegex()\n                val hqRegex = """"hq_src"\\s*:\\s*"([^"]+)"""".toRegex()'
content = content.replace(target1, rep1)

target2 = """                playableHdRegex.findAll(unescapedHtml).forEach { match ->
                    val urlStr = match.groups[1]?.value
                    if (!urlStr.isNullOrBlank()) {
                        val clean = urlStr.replace("&amp;", "&").replace("\\/", "/")
                        val resolved = resolveVideoDirectUrlRedirect(clean)
                        links.add(VideoLink("720p (Direct)", resolved, isHd = true, hasAudio = true, height = 720))
                    }
                }"""
rep2 = target2 + """
                hd1080Regex.findAll(unescapedHtml).forEach { match ->
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
                        links.add(VideoLink("1080p (HQ)", resolved, isHd = true, hasAudio = true, height = 1080))
                    }
                }"""
content = content.replace(target2, rep2)

with open('app/src/main/java/com/example/FacebookVideoExtractor.kt', 'w') as f:
    f.write(content)
