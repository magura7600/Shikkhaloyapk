import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# Replace the Scaffold to make it look better
target_scaffold = """        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(end = 48.dp) // Offset for back button to center text
                        ) {
                            Text(
                                text = "ক্লাসের লেকচার ফাইল:", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            if (pageCount > 0) {
                                Text(
                                    text = "পৃষ্ঠা: ${firstVisibleItemIndex + 1} / $pageCount",
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(androidx.compose.material.icons.filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                )
            },
            containerColor = Color(0xFFE8EDF1)
        ) { paddingValues ->"""

replacement_scaffold = """        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "ক্লাসের লেকচার ফাইল", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(androidx.compose.material.icons.filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                )
            },
            containerColor = Color(0xFFF1F5F9)
        ) { paddingValues ->"""

content = content.replace(target_scaffold, replacement_scaffold)

# Insert the floating page indicator inside the if (pageCount > 0) block
target_box = """                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            )
                        }
                        .transformable(state = state)
                ) {"""

replacement_box = """                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                androidx.compose.foundation.gestures.detectDragGestures { change, dragAmount ->
                                    if (scale > 1f) {
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    }
                                }
                            }
                            .transformable(state = state)
                    ) {"""

content = content.replace(target_box, replacement_box)

# Now we need to close the inner box and add the floating page indicator
target_list_close = """                        }
                    }
                }
            } else {"""

replacement_list_close = """                        }
                    }
                }
                
                // Floating Page Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(
                            color = Color(0xAA000000),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "পৃষ্ঠা: ${firstVisibleItemIndex + 1} / $pageCount",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {"""

content = content.replace(target_list_close, replacement_list_close)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)

