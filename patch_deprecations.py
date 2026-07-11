import os
import re

def replace_in_file(filepath):
    if not os.path.exists(filepath):
        return
        
    with open(filepath, 'r') as f:
        content = f.read()

    # Replace Divider with HorizontalDivider
    content = re.sub(r'\bDivider\(', 'HorizontalDivider(', content)
    
    # Replace Icons
    content = re.sub(r'Icons\.Filled\.MenuBook\b', 'Icons.AutoMirrored.Filled.MenuBook', content)
    content = re.sub(r'Icons\.Outlined\.MenuBook\b', 'Icons.AutoMirrored.Outlined.MenuBook', content)
    content = re.sub(r'Icons\.Filled\.HelpOutline\b', 'Icons.AutoMirrored.Filled.HelpOutline', content)
    content = re.sub(r'Icons\.Filled\.ArrowBack\b', 'Icons.AutoMirrored.Filled.ArrowBack', content)
    content = re.sub(r'Icons\.Filled\.ArrowForward\b', 'Icons.AutoMirrored.Filled.ArrowForward', content)
    content = re.sub(r'Icons\.Filled\.Article\b', 'Icons.AutoMirrored.Filled.Article', content)
    content = re.sub(r'Icons\.Outlined\.Logout\b', 'Icons.AutoMirrored.Outlined.Logout', content)
    content = re.sub(r'Icons\.Filled\.LibraryBooks\b', 'Icons.AutoMirrored.Filled.LibraryBooks', content)
    content = re.sub(r'Icons\.Filled\.Assignment\b', 'Icons.AutoMirrored.Filled.Assignment', content)
    content = re.sub(r'Icons\.Filled\.ListAlt\b', 'Icons.AutoMirrored.Filled.ListAlt', content)
    
    # Replace capitalize()
    content = re.sub(r'\.capitalize\(\)', '.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }', content)
    
    with open(filepath, 'w') as f:
        f.write(content)

for root, _, files in os.walk('app/src/main/java/com/example'):
    for file in files:
        if file.endswith('.kt'):
            replace_in_file(os.path.join(root, file))

