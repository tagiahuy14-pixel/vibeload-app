package com.vibeload.app.presentation.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

data class LiquidGlassTabItem(
    val title: String,
    val icon: ImageVector? = null,
    val count: Int? = null
)

/**
 * Apple Liquid Glass Segmented Control for Jetpack Compose.
 * Features an ultra-smooth sliding glass capsule thumb indicator powered by spring physics,
 * glass specular highlights, and horizontal swipe gesture support.
 */
@Composable
fun LiquidGlassSegmentedControl(
    items: List<LiquidGlassTabItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val containerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.72f) else Color(0xFFE2E8F0).copy(alpha = 0.75f)
    val containerBorder = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)

    val activeGlassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0071E3),
            Color(0xFF2997FF)
        )
    )

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(999.dp))
            .padding(4.dp)
            .pointerInput(selectedIndex, items.size) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDragX = 0f },
                    onDragEnd = {
                        val thresholdPx = 40.dp.toPx()
                        if (accumulatedDragX < -thresholdPx && selectedIndex < items.size - 1) {
                            VibeHaptic.tab(view)
                            onItemSelected(selectedIndex + 1)
                        } else if (accumulatedDragX > thresholdPx && selectedIndex > 0) {
                            VibeHaptic.tab(view)
                            onItemSelected(selectedIndex - 1)
                        }
                        accumulatedDragX = 0f
                    },
                    onDragCancel = { accumulatedDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        accumulatedDragX += dragAmount
                    }
                )
            }
    ) {
        val count = items.size.coerceAtLeast(1)
        val tabWidth = maxWidth / count
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "liquidGlassSliderOffset"
        )

        // Sliding Glass Indicator Capsule
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(999.dp),
                    spotColor = Color(0xFF0071E3).copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(999.dp))
                .background(activeGlassBrush)
                .border(
                    0.5.dp,
                    Color.White.copy(alpha = 0.5f),
                    RoundedCornerShape(999.dp)
                )
        )

        // Tab items (Z-index above indicator)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val textColor = if (isSelected) {
                    Color.White
                } else if (isDark) {
                    Color(0xFF94A3B8)
                } else {
                    Color(0xFF64748B)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            if (index != selectedIndex) {
                                VibeHaptic.tab(view)
                                onItemSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        item.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = item.title,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.count?.let { cnt ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.25f)
                                        else if (isDark) Color.White.copy(alpha = 0.1f)
                                        else Color.Black.copy(alpha = 0.08f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = cnt.toString(),
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
