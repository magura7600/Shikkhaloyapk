import re

with open('app/src/main/java/com/example/VideoPlayer.kt', 'r') as f:
    content = f.read()

content = content.replace('enterPictureInPictureMode()', 'enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())')

with open('app/src/main/java/com/example/VideoPlayer.kt', 'w') as f:
    f.write(content)
