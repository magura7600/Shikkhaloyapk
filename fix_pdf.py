import re

with open('app/src/main/java/com/example/PdfViewer.kt', 'r') as f:
    content = f.read()

# 1. Update PdfViewer to allow scrolling and fix spacing
target1 = """                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = scale == 1f // Only allow scrolling when not zoomed
                    ) {"""

replacement1 = """                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        userScrollEnabled = true // Always allow scrolling to load new pages
                    ) {"""
content = content.replace(target1, replacement1)

# 2. Update PdfPageSimple Card to fill screen and merge
target2 = """    Card(
        modifier = Modifier.fillMaxWidth().clipToBounds(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {"""

replacement2 = """    Card(
        modifier = Modifier.fillMaxWidth().clipToBounds(),
        shape = androidx.compose.ui.graphics.RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/PdfViewer.kt', 'w') as f:
    f.write(content)
