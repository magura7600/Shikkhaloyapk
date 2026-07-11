import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("""            } else if (pageCount > 0 && pdfRenderer != null) {
            pdfRenderer?.let { validRenderer ->
                val pagerState = rememberPagerState(pageCount = { pageCount })
                val coroutineScope = rememberCoroutineScope()
                var isPagerScrollEnabled by remember { mutableStateOf(true) }
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = isPagerScrollEnabled,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp
                ) { index ->
                    ZoomablePdfPage(
                        pdfRenderer = validRenderer,
                        pageIndex = index,
                        bitmapCache = bitmapCache,
                        onZoomChanged = { isZoomed ->
                            isPagerScrollEnabled = !isZoomed
                        },
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }""", """            } else if (pageCount > 0 && pdfRenderer != null) {
                val pagerState = rememberPagerState(pageCount = { pageCount })
                val coroutineScope = rememberCoroutineScope()
                var isPagerScrollEnabled by remember { mutableStateOf(true) }
                pdfRenderer?.let { validRenderer ->
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = isPagerScrollEnabled,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp
                ) { index ->
                    ZoomablePdfPage(
                        pdfRenderer = validRenderer,
                        pageIndex = index,
                        bitmapCache = bitmapCache,
                        onZoomChanged = { isZoomed ->
                            isPagerScrollEnabled = !isZoomed
                        },
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }
                }""")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
