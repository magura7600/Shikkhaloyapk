package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun CustomBottomNavigation(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF2D2D2D))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                
                val transition = updateTransition(targetState = isSelected, label = "tab transition")
                
                val alpha by transition.animateFloat(
                    transitionSpec = { tween(durationMillis = 300) },
                    label = "alpha"
                ) { if (it) 1f else 0f }
                
                val yOffset by transition.animateDp(
                    transitionSpec = { tween(durationMillis = 300, easing = FastOutSlowInEasing) },
                    label = "yOffset"
                ) { if (it) 0.dp else 10.dp }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                // Spotlight effect
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .fillMaxHeight()
                                        .align(Alignment.TopCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    item.color.copy(alpha = 0.4f * alpha),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                // Top line
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(3.dp)
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                        .background(item.color.copy(alpha = alpha))
                                )
                            }
                            
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) item.color else Color.Gray,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(y = if (isSelected) (-2).dp else 0.dp)
                            )
                        }
                        
                        if (item.title.isNotEmpty()) {
                            Text(
                                text = item.title,
                                color = if (isSelected) item.color else Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.offset(y = if (isSelected) (-4).dp else 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
