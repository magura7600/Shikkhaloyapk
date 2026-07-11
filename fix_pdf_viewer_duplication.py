import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# The file got duplicated blocks due to regex replace, let's just replace the whole if-else block
start_idx = content.find("} else if (pageCount > 0 && pdfRenderer != null) {")
end_idx = content.find("} else {\n            Box(\n                modifier = Modifier.fillMaxSize(),")

if start_idx != -1 and end_idx != -1:
    new_block = """} else if (pageCount > 0 && pdfRenderer != null) {
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

            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pageCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clickable { showJumpToPageDialog = true }
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / $pageCount পৃষ্ঠা",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                if (activity != null) {
                                    isLandscape = !isLandscape
                                    activity.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(
                                if (isLandscape) Icons.Default.ScreenRotation else Icons.Default.ScreenRotation, 
                                contentDescription = "Rotate", 
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            if (showJumpToPageDialog) {
                AlertDialog(
                    onDismissRequest = { showJumpToPageDialog = false },
                    title = { Text("পৃষ্ঠা নম্বর লিখুন") },
                    text = {
                        OutlinedTextField(
                            value = jumpPageInput,
                            onValueChange = { jumpPageInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("১ থেকে $pageCount এর মধ্যে") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val page = jumpPageInput.toIntOrNull()
                            if (page != null && page in 1..pageCount) {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(page - 1)
                                }
                            }
                            showJumpToPageDialog = false
                        }) {
                            Text("যান")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJumpToPageDialog = false }) {
                            Text("বাতিল")
                        }
                    }
                )
            }
        """
    
    content = content[:start_idx] + new_block + content[end_idx:]
    with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
        f.write(content)

