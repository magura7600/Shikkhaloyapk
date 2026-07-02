package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShikkhaloyLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 140.dp,
    showText: Boolean = true,
    darkTheme: Boolean = false
) {
    val textColor = if (darkTheme) Color.White else Color(0xFF1E3A8A)
    val sloganColor = if (darkTheme) Color(0xFFE5E7EB) else Color(0xFF4B5563)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Vector drawn dynamically on Canvas
        Box(
            modifier = Modifier
                .size(iconSize)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo_vector),
                contentDescription = "Shikkhaloy Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
        
        if (showText) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "শিক্ষালয়",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Custom Curve Line representing Growth & Progress
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F766E), Color(0xFFF4B400), Color(0xFF1E3A8A))
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "শিক্ষা হোক সহজ, সফল হোক জীবনের লক্ষ্য",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = sloganColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShikkhaloySplashScreen() {
    // Elegant pulsing animation for loading indicator and branding
    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransition")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Dynamic simulated loading progress
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val duration = 2200f
        val steps = 50
        val delayTime = (duration / steps).toLong()
        for (i in 1..steps) {
            kotlinx.coroutines.delay(delayTime)
            progress = i / steps.toFloat()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F766E), // Deep Teal
                        Color(0xFF134E5E), // Teal-Blue
                        Color(0xFF1E3A8A)  // Deep Navy Blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background lighting (glow effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x22F4B400), // Glowing Gold
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Scale Animation for custom logo
            val scaleAnim by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )

            // Custom Vector logo drawn dynamically on Canvas
            ShikkhaloyLogo(
                showText = false,
                iconSize = 180.dp,
                darkTheme = true,
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scaleAnim,
                        scaleY = scaleAnim,
                        alpha = alphaAnim
                    )
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Progress Indicators matching the launcher branding reference
            Column(
                modifier = Modifier.width(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Loading Text with smooth pulse
                Text(
                    text = "লোড হচ্ছে...",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = alphaAnim),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Sleek styled Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2DD4BF), // Mint
                                        Color(0xFFF4B400)  // Gold
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
