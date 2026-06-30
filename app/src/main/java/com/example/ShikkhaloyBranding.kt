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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Color Palette
                val navy = Color(0xFF1E3A8A)
                val teal = Color(0xFF0F766E)
                val gold = Color(0xFFF4B400)
                val logoNavy = if (darkTheme) Color.White else navy
                val logoTeal = if (darkTheme) Color(0xFF2DD4BF) else teal
                val logoGold = gold

                // 1. Draw Glowing Lightbulb Center (Gold/Yellow Circle)
                val bulbRadius = w * 0.22f
                val bulbCenterY = h * 0.45f
                drawCircle(
                    color = logoGold,
                    radius = bulbRadius,
                    center = androidx.compose.ui.geometry.Offset(w * 0.5f, bulbCenterY)
                )
                
                // 2. Draw Lightbulb Outer Ring (Teal/Mint Arc)
                drawArc(
                    color = logoTeal,
                    startAngle = -30f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, bulbCenterY - w * 0.28f),
                    size = androidx.compose.ui.geometry.Size(w * 0.56f, w * 0.56f)
                )
                
                // 3. Draw Rays of Light (Gold lines)
                val rayLength = w * 0.08f
                val rayWidth = w * 0.03f
                // Right side rays representing learning light
                for (angle in listOf(-45f, 0f, 45f)) {
                    val rad = Math.toRadians(angle.toDouble())
                    val startX = (w * 0.5f + (bulbRadius * 1.5f) * Math.cos(rad)).toFloat()
                    val startY = (bulbCenterY + (bulbRadius * 1.5f) * Math.sin(rad)).toFloat()
                    val endX = (startX + rayLength * Math.cos(rad)).toFloat()
                    val endY = (startY + rayLength * Math.sin(rad)).toFloat()
                    drawLine(
                        color = logoGold,
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = rayWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                
                // 4. Draw Pencil Tip / Bottom Thread (Teal/Mint and Gold)
                val threadY = bulbCenterY + bulbRadius * 1.05f
                val threadWidth = w * 0.22f
                val threadHeight = h * 0.05f
                // 3 thread segments representing pencil lines
                for (i in 0..2) {
                    val currY = threadY + i * (threadHeight * 1.2f)
                    drawLine(
                        color = logoTeal,
                        start = androidx.compose.ui.geometry.Offset(w * 0.5f - threadWidth * 0.5f, currY),
                        end = androidx.compose.ui.geometry.Offset(w * 0.5f + threadWidth * 0.5f, currY),
                        strokeWidth = w * 0.04f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                
                // Draw Pencil Lead Point (Gold triangle pointing down)
                val tipY = threadY + 3.2f * (threadHeight * 1.2f)
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f - threadWidth * 0.4f, tipY)
                    lineTo(w * 0.5f + threadWidth * 0.4f, tipY)
                    lineTo(w * 0.5f, tipY + h * 0.12f)
                    close()
                }
                drawPath(path = path, color = logoGold)
                
                // Draw Pencil lead core (navy tip)
                val leadPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f - threadWidth * 0.15f, tipY + h * 0.07f)
                    lineTo(w * 0.5f + threadWidth * 0.15f, tipY + h * 0.07f)
                    lineTo(w * 0.5f, tipY + h * 0.12f)
                    close()
                }
                drawPath(path = leadPath, color = if (darkTheme) navy else logoNavy)
                
                // 5. Draw Graduation Cap (Navy Blue Diamond) on top
                val capCenterY = h * 0.15f
                val capWidth = w * 0.65f
                val capHeight = h * 0.15f
                val capPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, capCenterY - capHeight * 0.5f) // top
                    lineTo(w * 0.5f + capWidth * 0.5f, capCenterY) // right
                    lineTo(w * 0.5f, capCenterY + capHeight * 0.5f) // bottom
                    lineTo(w * 0.5f - capWidth * 0.5f, capCenterY) // left
                    close()
                }
                drawPath(path = capPath, color = logoNavy)
                
                // Cap base/band
                val bandPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.35f, capCenterY + capHeight * 0.2f)
                    lineTo(w * 0.65f, capCenterY + capHeight * 0.2f)
                    lineTo(w * 0.6f, capCenterY + capHeight * 0.6f)
                    lineTo(w * 0.4f, capCenterY + capHeight * 0.6f)
                    close()
                }
                drawPath(path = bandPath, color = logoNavy)
                
                // Tassel hanging to the left
                val tasselPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, capCenterY)
                    quadraticTo(w * 0.24f, capCenterY + h * 0.05f, w * 0.22f, capCenterY + h * 0.22f)
                }
                drawPath(
                    path = tasselPath,
                    color = logoGold,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.02f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                // Tassel hanging ornament (gold circle/pill)
                drawCircle(
                    color = logoGold,
                    radius = w * 0.03f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.21f, capCenterY + h * 0.24f)
                )
            }
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
            // Main App Logo
            ShikkhaloyLogo(
                iconSize = 160.dp,
                showText = true,
                darkTheme = true
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
