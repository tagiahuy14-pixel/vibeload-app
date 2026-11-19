package com.vibeload.app.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class LiquidGlassTabItem(
    val title: String,
    val icon: ImageVector? = null,
    val count: Int? = null
)

/**
 * Apple Liquid Glass Segmented Control for Jetpack Compose.
 * Features an ultra-smooth sliding glass capsule thumb indicator powered by spring physics,
 * glass specular highlights, and real-time interactive gliding/dragging across buttons with haptic feedback
 * (matching Samsung OneUI & iOS dynamic segmented control).
 */
@Composable
fun LiquidGlassSegmentedControl(
    items: List<LiquidGlassTabItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false
) {
    if (items.isEmpty()) return

    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val count = items.size.coerceAtLeast(1)
    val coroutineScope = rememberCoroutineScope()

    val currentSelectedIndex by rememberUpdatedState(selectedIndex.coerceIn(0, count - 1))
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)

    // Animated fractional position [0.0f .. (count - 1).toFloat()]
    val tabPosition = remember { Animatable(currentSelectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync with external selectedIndex changes when not dragging
    LaunchedEffect(currentSelectedIndex) {
        if (!isDragging && tabPosition.targetValue != currentSelectedIndex.toFloat()) {
            tabPosition.animateTo(
                targetValue = currentSelectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val controlHeight = if (isVertical) 52.dp else 44.dp

    val containerBg = if (isDark) {
        Color(0xFF161E2E).copy(alpha = 0.85f)
    } else {
        Color(0xFFE2E8F0).copy(alpha = 0.80f)
    }
    val containerBorder = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)

    val activeGlassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0071E3),
            Color(0xFF2997FF)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(controlHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(999.dp))
            .padding(3.dp)
            .pointerInput(count) {
                val touchSlop = viewConfiguration.touchSlop
                val tabWidthPx = size.width.toFloat() / count
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var activeIndex = (startX / tabWidthPx).toInt().coerceIn(0, count - 1)
                    var hasDragged = false

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val currentX = change.position.x
                            val deltaX = abs(currentX - startX)

                            if (!hasDragged && deltaX > touchSlop) {
                                hasDragged = true
                                isDragging = true
                            }

                            if (hasDragged) {
                                val currentFraction = ((currentX - tabWidthPx / 2f) / tabWidthPx).coerceIn(0f, (count - 1).toFloat())
                                coroutineScope.launch {
                                    tabPosition.snapTo(currentFraction)
                                }

                                val hoveredIndex = currentFraction.roundToInt().coerceIn(0, count - 1)
                                if (hoveredIndex != activeIndex) {
                                    activeIndex = hoveredIndex
                                    VibeHaptic.tab(view)
                                }
                                change.consume()
                            }
                        }
                    } finally {
                        isDragging = false
                        val finalIndex = if (hasDragged) {
                            tabPosition.value.roundToInt().coerceIn(0, count - 1)
                        } else {
                            (startX / tabWidthPx).toInt().coerceIn(0, count - 1)
                        }

                        if (!hasDragged) {
                            VibeHaptic.tab(view)
                        }
                        currentOnItemSelected(finalIndex)
                        coroutineScope.launch {
                            tabPosition.animateTo(
                                targetValue = finalIndex.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                }
            }
    ) {
        val tabWidthDp = maxWidth / count
        val indicatorOffsetDp = tabWidthDp * tabPosition.value

        // Sliding Glass Indicator Capsule
        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetDp)
                .width(tabWidthDp)
                .fillMaxHeight()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(999.dp),
                    spotColor = Color(0xFF0071E3).copy(alpha = 0.40f)
                )
                .clip(RoundedCornerShape(999.dp))
                .background(activeGlassBrush)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.45f),
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
            val visualSelectedIndex = tabPosition.value.roundToInt().coerceIn(0, count - 1)

            items.forEachIndexed { index, item ->
                val isSelected = index == visualSelectedIndex
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
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVertical) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            item.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Symmetrical, evenly spaced Horizontal Layout
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            item.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.5.dp))
                            }
                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = 11.5.sp,
                                letterSpacing = (-0.2).sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                            if (item.count != null && item.count > 0) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "(${item.count})",
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else textColor.copy(alpha = 0.75f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
