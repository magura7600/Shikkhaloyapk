import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

content = content.replace("""        } else if (pageCount > 0 && pdfRenderer != null) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pageCount })
            val coroutineScope = rememberCoroutineScope()
            var isPagerScrollEnabled by remember { mutableStateOf(true) }
            val validRenderer = pdfRenderer
            if (validRenderer != null) {
                androidx.compose.foundation.pager.HorizontalPager(
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
            }
            androidx.compose.animation.AnimatedVisibility(""", """        } else if (pageCount > 0 && pdfRenderer != null) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pageCount })
            val coroutineScope = rememberCoroutineScope()
            var isPagerScrollEnabled by remember { mutableStateOf(true) }
            val validRenderer = pdfRenderer
            if (validRenderer != null) {
                androidx.compose.foundation.pager.HorizontalPager(
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
            }
            
            // Allow AnimatedVisibility and its children to access pagerState
            androidx.compose.animation.AnimatedVisibility(""")

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
