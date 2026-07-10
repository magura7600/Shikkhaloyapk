package com.example

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
fun InteractiveBear(
    isPasswordFocused: Boolean,
    showPassword: Boolean,
    emailLength: Int,
    isEmailFocused: Boolean,
    modifier: Modifier = Modifier
) {
    // Smooth animation transitions
    val coverProgress by animateFloatAsState(
        targetValue = if (isPasswordFocused) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "coverProgress"
    )

    val peekProgress by animateFloatAsState(
        targetValue = if (isPasswordFocused && showPassword) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "peekProgress"
    )

    val lookXTarget = if (isEmailFocused) {
        // Look left to right depending on email length
        ((emailLength.coerceAtMost(25) / 25f) * 12f) - 6f
    } else {
        0f
    }

    val lookYTarget = if (isEmailFocused) {
        4f // look down slightly
    } else {
        0f
    }

    val lookX by animateFloatAsState(
        targetValue = lookXTarget,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "lookX"
    )

    val lookY by animateFloatAsState(
        targetValue = lookYTarget,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "lookY"
    )

    Canvas(
        modifier = modifier
            .size(140.dp)
            .padding(4.dp)
    ) {
        val width = size.width
        val height = size.height

        val centerX = width / 2f
        val centerY = height / 2f + 12f

        val headRadius = width * 0.35f

        // 1. DRAW EARS
        val earRadius = headRadius * 0.28f
        val leftEarX = centerX - headRadius * 0.75f
        val leftEarY = centerY - headRadius * 0.75f
        val rightEarX = centerX + headRadius * 0.75f
        val rightEarY = centerY - headRadius * 0.75f

        // Left Ear outer
        drawCircle(
            color = Color(0xFF8D5B4C),
            radius = earRadius,
            center = Offset(leftEarX, leftEarY)
        )
        // Left Ear inner
        drawCircle(
            color = Color(0xFFF3A683),
            radius = earRadius * 0.6f,
            center = Offset(leftEarX, leftEarY)
        )

        // Right Ear outer
        drawCircle(
            color = Color(0xFF8D5B4C),
            radius = earRadius,
            center = Offset(rightEarX, rightEarY)
        )
        // Right Ear inner
        drawCircle(
            color = Color(0xFFF3A683),
            radius = earRadius * 0.6f,
            center = Offset(rightEarX, rightEarY)
        )

        // 2. DRAW LITTLE PURPLE HAT (styled like the logo/image)
        val hatRadius = headRadius * 0.28f
        val hatCenterX = centerX
        val hatCenterY = centerY - headRadius + 4f
        val hatPath = Path().apply {
            arcTo(
                rect = Rect(
                    left = hatCenterX - hatRadius,
                    top = hatCenterY - hatRadius,
                    right = hatCenterX + hatRadius,
                    bottom = hatCenterY + hatRadius
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(
            path = hatPath,
            color = Color(0xFF818CF8) // indigo light purple
        )
        // Tiny pompom on the hat
        drawCircle(
            color = Color(0xFF6366F1),
            radius = 6f,
            center = Offset(hatCenterX, hatCenterY - hatRadius)
        )

        // 3. DRAW HEAD (Face base)
        drawCircle(
            color = Color(0xFF8D5B4C),
            radius = headRadius,
            center = Offset(centerX, centerY)
        )

        // 4. DRAW MUZZLE (Beige area for nose & mouth)
        val muzzleWidth = headRadius * 0.8f
        val muzzleHeight = headRadius * 0.55f
        val muzzleX = centerX - muzzleWidth / 2f
        val muzzleY = centerY + headRadius * 0.2f
        drawOval(
            color = Color(0xFFF5EBE0),
            topLeft = Offset(muzzleX, muzzleY),
            size = Size(muzzleWidth, muzzleHeight)
        )

        // 5. DRAW NOSE
        val noseWidth = headRadius * 0.22f
        val noseHeight = headRadius * 0.14f
        val noseX = centerX - noseWidth / 2f
        val noseY = centerY + headRadius * 0.26f
        drawOval(
            color = Color(0xFF2D1A18),
            topLeft = Offset(noseX, noseY),
            size = Size(noseWidth, noseHeight)
        )

        // 6. DRAW MOUTH (smile)
        val mouthY = noseY + noseHeight + 2f
        val mouthPath = Path().apply {
            moveTo(centerX - 12f, mouthY)
            quadraticBezierTo(centerX - 6f, mouthY + 8f, centerX, mouthY)
            quadraticBezierTo(centerX + 6f, mouthY + 8f, centerX + 12f, mouthY)
        }
        drawPath(
            path = mouthPath,
            color = Color(0xFF2D1A18),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // 7. DRAW EYES (Reacting to coverProgress & peekProgress)
        val eyeLeftX = centerX - headRadius * 0.36f
        val eyeRightX = centerX + headRadius * 0.36f
        val eyeY = centerY - headRadius * 0.12f

        // --- LEFT EYE ---
        if (coverProgress < 0.95f || peekProgress > 0.05f) {
            val currentEyeRadius = headRadius * 0.12f
            val currentPupilRadius = headRadius * 0.07f

            // Sclera (White background of eye)
            drawCircle(
                color = Color.White,
                radius = currentEyeRadius,
                center = Offset(eyeLeftX, eyeY)
            )

            val pLookX = if (peekProgress > 0.05f) 0f else lookX
            val pLookY = if (peekProgress > 0.05f) -2f else lookY
            
            drawCircle(
                color = Color(0xFF2D1A18),
                radius = currentPupilRadius,
                center = Offset(eyeLeftX + pLookX, eyeY + pLookY)
            )

            // Eye highlight (tiny white dot)
            drawCircle(
                color = Color.White,
                radius = currentPupilRadius * 0.35f,
                center = Offset(eyeLeftX + pLookX - 2f, eyeY + pLookY - 2f)
            )
        } else {
            // Left eye is closed (happy closed arc)
            val leftEyePath = Path().apply {
                moveTo(eyeLeftX - 12f, eyeY + 2f)
                quadraticBezierTo(eyeLeftX, eyeY - 6f, eyeLeftX + 12f, eyeY + 2f)
            }
            drawPath(
                path = leftEyePath,
                color = Color(0xFF2D1A18),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        // --- RIGHT EYE ---
        if (coverProgress < 0.85f) {
            val currentEyeRadius = headRadius * 0.12f
            val currentPupilRadius = headRadius * 0.07f

            // Sclera
            drawCircle(
                color = Color.White,
                radius = currentEyeRadius,
                center = Offset(eyeRightX, eyeY)
            )

            // Pupil
            drawCircle(
                color = Color(0xFF2D1A18),
                radius = currentPupilRadius,
                center = Offset(eyeRightX + lookX, eyeY + lookY)
            )

            // Eye highlight
            drawCircle(
                color = Color.White,
                radius = currentPupilRadius * 0.35f,
                center = Offset(eyeRightX + lookX - 2f, eyeY + lookY - 2f)
            )
        } else {
            // Right eye is closed (happy squint arc)
            val rightEyePath = Path().apply {
                moveTo(eyeRightX - 12f, eyeY + 2f)
                quadraticBezierTo(eyeRightX, eyeY - 6f, eyeRightX + 12f, eyeY + 2f)
            }
            drawPath(
                path = rightEyePath,
                color = Color(0xFF2D1A18),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        // 8. DRAW PAWS (Hands)
        val defaultLeftPawX = centerX - headRadius * 0.55f
        val defaultLeftPawY = centerY + headRadius * 0.8f

        val defaultRightPawX = centerX + headRadius * 0.55f
        val defaultRightPawY = centerY + headRadius * 0.8f

        val coverLeftPawX = eyeLeftX
        val coverLeftPawY = eyeY

        val coverRightPawX = eyeRightX
        val coverRightPawY = eyeY

        val targetLeftPawX = if (peekProgress > 0.01f) {
            lerp(coverLeftPawX, defaultLeftPawX, 0.45f) - 10f
        } else {
            lerp(defaultLeftPawX, coverLeftPawX, coverProgress)
        }

        val targetLeftPawY = if (peekProgress > 0.01f) {
            lerp(coverLeftPawY, defaultLeftPawY, 0.45f) + 15f
        } else {
            lerp(defaultLeftPawY, coverLeftPawY, coverProgress)
        }

        val targetRightPawX = lerp(defaultRightPawX, coverRightPawX, coverProgress)
        val targetRightPawY = lerp(defaultRightPawY, coverRightPawY, coverProgress)

        val pawRadius = headRadius * 0.25f

        // Draw Left Paw
        drawCircle(
            color = Color(0xFF8D5B4C),
            radius = pawRadius,
            center = Offset(targetLeftPawX, targetLeftPawY)
        )
        // Left Paw Pad
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.5f,
            center = Offset(targetLeftPawX, targetLeftPawY + 2f)
        )
        // Left Paw toes
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.16f,
            center = Offset(targetLeftPawX - 10f, targetLeftPawY - 10f)
        )
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.16f,
            center = Offset(targetLeftPawX, targetLeftPawY - 14f)
        )
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.16f,
            center = Offset(targetLeftPawX + 10f, targetLeftPawY - 10f)
        )

        // Draw Right Paw
        drawCircle(
            color = Color(0xFF8D5B4C),
            radius = pawRadius,
            center = Offset(targetRightPawX, targetRightPawY)
        )
        // Right Paw Pad
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.5f,
            center = Offset(targetRightPawX, targetRightPawY + 2f)
        )
        // Right Paw toes
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.16f,
            center = Offset(targetRightPawX - 10f, targetRightPawY - 10f)
        )
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.16f,
            center = Offset(targetRightPawX, targetRightPawY - 14f)
        )
        drawCircle(
            color = Color(0xFFF3A683),
            radius = pawRadius * 0.16f,
            center = Offset(targetRightPawX + 10f, targetRightPawY - 10f)
        )
    }
}
