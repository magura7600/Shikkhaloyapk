with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

target = """    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {"""

replacement = """    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onBack() },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = androidx.compose.ui.graphics.Color(0xFF1E293B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            androidx.compose.material3.Text(
                text = clazz.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(42.dp))
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
        f.write(content)
    print("Replaced!")
else:
    print("Target not found.")
