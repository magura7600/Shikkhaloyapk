import sys

def main():
    file_path = "app/src/main/java/com/example/PdfViewer.kt"
    with open(file_path, "r") as f:
        content = f.read()

    target = """                        Text("পিডিএফ ফাইলটি লোড করা যায়নি।", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (pageCount > 0 && pdfRenderer != null) {"""

    replacement = """                        Text("পিডিএফ ফাইলটি লোড করা যায়নি।", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced pdf error successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
