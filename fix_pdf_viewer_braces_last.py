import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("""                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}""", """                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }""")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
