import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

# Replace the closing brace of the video section and add a new Column for the rest of the content
video_end = r"""                    }
                }
                
                Spacer\(modifier = Modifier\.height\(16\.dp\)\)
                
                if \(clazz\.recordedLink\.isNotBlank\(\)\) \{"""

new_video_end = """                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (clazz.recordedLink.isNotBlank()) {"""

content = re.sub(video_end, new_video_end, content)

# Now find the end of the Class Detail view to close this new Column.
# It ends right before `} else if (clazz.liveLink.isNotBlank() || clazz.recordedLink.isBlank()) {`
class_detail_end = r"""                }
            }
        \} else if \(clazz\.liveLink\.isNotBlank\(\) \|\| clazz\.recordedLink\.isBlank\(\)\) \{"""

new_class_detail_end = """                }
            }
            } // Close the padded column
        } else if (clazz.liveLink.isNotBlank() || clazz.recordedLink.isBlank()) {"""

content = re.sub(class_detail_end, new_class_detail_end, content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
