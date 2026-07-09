import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# 1. Update root Column padding
old_column = r"""    Column\(
        modifier = Modifier
            \.fillMaxWidth\(\)
            \.statusBarsPadding\(\)
            \.verticalScroll\(rememberScrollState\(\)\)
            \.padding\(bottom = 32\.dp, start = 16\.dp, end = 16\.dp, top = 12\.dp\)
    \) \{"""

new_column = """    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp, top = 12.dp)
    ) {"""
content = re.sub(old_column, new_column, content)

# 2. Add padding to Top Bar
old_topbar = r"""        // Top Bar with Back Button
        Row\(
            modifier = Modifier
                \.fillMaxWidth\(\)
                \.padding\(vertical = 12\.dp\),"""

new_topbar = """        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 12.dp),"""
content = re.sub(old_topbar, new_topbar, content)

# 3. Add padding to the content below video
# The video section ends with Spacer(modifier = Modifier.height(16.dp))
# Then we have Download Button, then Tabs, then content.
# Actually, everything below the video should probably just be in a Column with 16.dp padding.

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
