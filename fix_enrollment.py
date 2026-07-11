with open('app/src/main/java/com/example/EnrollmentRequestsScreen.kt', 'r') as f:
    content = f.read()

old_block = """                    onClick = {
                        val req = showRejectDialogFor!!
                        coroutineScope.launch {"""

new_block = """                    onClick = {
                        val req = showRejectDialogFor
                        if (req != null) {
                            coroutineScope.launch {"""

old_block2 = """                            isProcessing = false
                            showRejectDialogFor = null
                        }
                    }"""

new_block2 = """                            isProcessing = false
                            showRejectDialogFor = null
                        }
                        }
                    }"""

if old_block in content and old_block2 in content:
    content = content.replace(old_block, new_block)
    content = content.replace(old_block2, new_block2)
    with open('app/src/main/java/com/example/EnrollmentRequestsScreen.kt', 'w') as f:
        f.write(content)
    print("Fixed EnrollmentRequestsScreen")
else:
    print("Block not found in EnrollmentRequestsScreen")
