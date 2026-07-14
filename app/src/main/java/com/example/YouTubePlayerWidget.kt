package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YouTubePlayerWidget(
    videoId: String,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onFullscreenToggle: (Boolean) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                YouTubePlayerView(context).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    
                    addFullscreenListener(object : FullscreenListener {
                        override fun onEnterFullscreen(fullscreenView: android.view.View, exitFullscreen: () -> Unit) {
                            onFullscreenToggle(true)
                        }
                        override fun onExitFullscreen() {
                            onFullscreenToggle(false)
                        }
                    })

                    val listener = object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                    }
                    
                    val options = IFramePlayerOptions.Builder().controls(1).fullscreen(1).build()
                    initialize(listener, options)
                }
            },
            update = { view ->
            }
        )
    }
}
