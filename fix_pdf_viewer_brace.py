import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# Let's count braces to fix this mess properly
content = re.sub(r'        } else if \(pageCount > 0 && pdfRenderer != null\) \{([\s\S]*?)        \} else \{\n            Box\(', r'        } else if (pageCount > 0 && pdfRenderer != null) {\n            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pageCount })\n            val coroutineScope = rememberCoroutineScope()\n            var isPagerScrollEnabled by remember { mutableStateOf(true) }\n            val validRenderer = pdfRenderer\n            if (validRenderer != null) {\n                androidx.compose.foundation.pager.HorizontalPager(\n                    state = pagerState,\n                    userScrollEnabled = isPagerScrollEnabled,\n                    modifier = Modifier.fillMaxSize(),\n                    pageSpacing = 16.dp\n                ) { index ->\n                    ZoomablePdfPage(\n                        pdfRenderer = validRenderer,\n                        pageIndex = index,\n                        bitmapCache = bitmapCache,\n                        onZoomChanged = { isZoomed ->\n                            isPagerScrollEnabled = !isZoomed\n                        },\n                        onTap = { controlsVisible = !controlsVisible }\n                    )\n                }\n            }\n' + r'\1        } else {\n            Box(', content)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
