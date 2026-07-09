import re

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'r') as f:
    content = f.read()

live_start = r"""        \} else if \(clazz\.liveLink\.isNotBlank\(\) \|\| clazz\.recordedLink\.isBlank\(\)\) \{
            // Live/Countdown/Waiting UI"""
new_live_start = """        } else if (clazz.liveLink.isNotBlank() || clazz.recordedLink.isBlank()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Live/Countdown/Waiting UI"""

content = re.sub(live_start, new_live_start, content)

live_end = r"""                        \}
                    \}
                \}
            \}
        \}
        
        Spacer\(modifier = Modifier\.height\(16\.dp\)\)
        
        if \(clazz\.pdfLinks\.isNotEmpty\(\)\) \{"""

new_live_end = """                        }
                    }
                }
            }
            } // Close Live padded column
        }
        
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        
        if (clazz.pdfLinks.isNotEmpty()) {"""

content = re.sub(live_end, new_live_end, content)

# Close the new Column at the end of the selectedClassForView block
class_view_end = r"""                \}
            \}
        \}
    \} else if \(selectedChapterForView != null\) \{"""

new_class_view_end = """                }
            }
        }
        } // Close bottom padded column
    } else if (selectedChapterForView != null) {"""

content = re.sub(class_view_end, new_class_view_end, content)

with open('app/src/main/java/com/example/CourseDetailScreen.kt', 'w') as f:
    f.write(content)
