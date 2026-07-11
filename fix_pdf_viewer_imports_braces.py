import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# Fix braces before @Composable
content = content.replace("""            }
        }
}

@Composable
fun ZoomablePdfPage""", """            }
        }
    }
}

@Composable
fun ZoomablePdfPage""")

# Fix long unresolved androidx imports
content = content.replace("androidx.compose.foundation.shape.CircleShape", "CircleShape")
content = content.replace("androidx.compose.material3.Icon", "Icon")
content = content.replace("androidx.compose.material.icons.Icons.Filled.Close", "Icons.Default.Close")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
