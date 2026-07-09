import sys

def fix_course_detail():
    file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
    with open(file_path, "r") as f:
        content = f.read()

    # Replace onError blocks
    target_error = """                                                onError = { errMsg ->
                                                    downloadingPdfUrl = null
                                                    Toast.makeText(context, "সরাসরি ভিউ করা যায়নি, ব্রাউজারে ওপেন করা হচ্ছে...", Toast.LENGTH_SHORT).show()
                                                    openBrowserIntent(pdf.url)
                                                }"""
    replacement_error = """                                                onError = { errMsg ->
                                                    downloadingPdfUrl = null
                                                    Toast.makeText(context, "পিডিএফ লোড করতে সমস্যা হচ্ছে।", Toast.LENGTH_SHORT).show()
                                                }"""
    if target_error in content:
        content = content.replace(target_error, replacement_error)
        print("CourseDetailScreen: onError fixed")

    # Replace isCloudOrWebUrl blocks
    target_cloud_1 = """                            val isCloudOrWebUrl = remember {
                                { url: String ->
                                    val lower = url.trim().lowercase()
                                    lower.contains("onedrive.live.com")
                                }
                            }"""
    replacement_cloud_1 = """                            val isCloudOrWebUrl = remember { { url: String -> false } }"""
    if target_cloud_1 in content:
        content = content.replace(target_cloud_1, replacement_cloud_1)
        print("CourseDetailScreen: isCloudOrWebUrl 1 fixed")

    target_cloud_2 = """                    val isCloudOrWebUrl = remember {
                        { url: String ->
                            val lower = url.trim().lowercase()
                            lower.contains("onedrive.live.com")
                        }
                    }"""
    replacement_cloud_2 = """                    val isCloudOrWebUrl = remember { { url: String -> false } }"""
    if target_cloud_2 in content:
        content = content.replace(target_cloud_2, replacement_cloud_2)
        print("CourseDetailScreen: isCloudOrWebUrl 2 fixed")

    with open(file_path, "w") as f:
        f.write(content)

def fix_pdf_viewer():
    file_path = "app/src/main/java/com/example/PdfViewer.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """                        Text("পিডিএফ ফাইলটি লোড করা যায়নি।", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (url.isNotBlank()) {
                            Button(onClick = {
                                try {
                                    val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fixedUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))) {
                                Text("ব্রাউজারে ওপেন করুন")
                            }
                        }
                    }
                }
            } else if (pageCount > 0 && pdfRenderer != null) {"""
    replacement = """                        Text("পিডিএফ ফাইলটি লোড করা যায়নি।", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (pageCount > 0 && pdfRenderer != null) {"""
        
    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("PdfViewer: fixed")

def fix_offline_manager():
    file_path = "app/src/main/java/com/example/OfflineDownloadManager.kt"
    with open(file_path, "r") as f:
        content = f.read()
        
    start_idx = content.find("fun sanitizeUrl(url: String): String {")
    if start_idx != -1:
        new_content = content[:start_idx] + """fun sanitizeUrl(url: String): String {
    return url.trim().replace(" ", "%20")
}
"""
        with open(file_path, "w") as f:
            f.write(new_content)
        print("OfflineDownloadManager: sanitizeUrl fixed")

if __name__ == "__main__":
    fix_course_detail()
    fix_pdf_viewer()
    fix_offline_manager()
