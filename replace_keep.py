import sys

def main():
    file_path = "app/src/main/java/com/example/CourseDetailScreen.kt"
    with open(file_path, "r") as f:
        content = f.read()
    
    target = """    // Set playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Monitor playback progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(250L)
        }
    }

    // Observe player listener
    DisposableEffect(exoPlayer) {"""

    replacement = """    // Set playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Monitor playback progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(250L)
        }
    }

    // Keep screen on while playing
    DisposableEffect(isPlaying, activity) {
        if (isPlaying) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Observe player listener
    DisposableEffect(exoPlayer) {"""

    if target in content:
        content = content.replace(target, replacement)
        with open(file_path, "w") as f:
            f.write(content)
        print("Replaced successfully!")
    else:
        print("Target not found.")

if __name__ == "__main__":
    main()
