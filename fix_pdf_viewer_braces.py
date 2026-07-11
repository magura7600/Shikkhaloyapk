import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# Fix the duplicate braces in ZoomablePdfPage
content = content.replace("    onTap: () -> Unit = {}) {) {", "    onTap: () -> Unit = {}\n) {")

# Fix the duplicate braces before @Composable
content = content.replace("""        }
}
@Composable
fun ZoomablePdfPage""", """        }
    }
}

@Composable
fun ZoomablePdfPage""")

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

# We need to completely rewrite the end of PdfViewer screen to fix braces
content = re.sub(r'                    \)\n                }\n            \} else \{\n                Box\(\n                    modifier = Modifier\.fillMaxSize\(\),\n                    contentAlignment = Alignment\.Center\n                \) \{\n                    CircularProgressIndicator\(color = MaterialTheme\.colorScheme\.primary\)\n                \}\n            \}\n        \}\n\}\n@Composable\nfun ZoomablePdfPage', r'                    )\n                }\n            } else {\n                Box(\n                    modifier = Modifier.fillMaxSize(),\n                    contentAlignment = Alignment.Center\n                ) {\n                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)\n                }\n            }\n        }\n    }\n}\n\n@Composable\nfun ZoomablePdfPage', content)

# Check EOF braces
if not content.strip().endswith("}"):
    content += "\n}"

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
