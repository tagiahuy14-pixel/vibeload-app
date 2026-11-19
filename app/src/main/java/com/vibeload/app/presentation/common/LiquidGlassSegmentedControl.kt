package com.vibeload.app.presentation.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * glass specular highlights, and real-time interactive gliding/dragging across buttons with haptic feedback
 * (matching Samsung OneUI & iOS dynamic segmented control).
 *
 * Supports two layout modes:
 * - Vertical (Icon on top, Label below): perfectly balanced, zero truncation, identical spacing for Vietnamese text.
 * - Horizontal (Icon and Label side-by-side): compact row for fewer or wider items.
 */
@Composable
fun LiquidGlassSegmentedControl(
    items: List<LiquidGlassTabItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean? = null
) {
    if (items.isEmpty()) return

    val view = LocalView.current
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()

    // Auto-detect vertical layout if 4 or more items and they all have icons to prevent Vietnamese text truncation
    val useVertical = isVertical ?: (items.size >= 4 && items.all { it.icon != null })
    val controlHeight = if (useVertical) 56.dp else 44.dp

    val containerBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.72f)
    } else {
        Color(0xFFE2E8F0).copy(alpha = 0.75f)
    }
    val containerBorder = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)

    val activeGlassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0071E3),
            Color(0xFF2997FF)
        )
    )

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(controlHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(999.dp))
            .padding(4.dp)
            .pointerInput(items.size, selectedIndex) {
                val count = items.size.coerceAtLeast(1)
                val contentWidthPx = size.width.toFloat()
                val tabWidthPx = contentWidthPx / count
                val touchSlop = viewConfiguration.touchSlop

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var currentX = startX
                    var activeIndex = (currentX / tabWidthPx).toInt().coerceIn(0, count - 1)
                    var hasDragged = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        currentX = change.position.x
                        val diff = kotlin.math.abs(currentX - startX)
                        if (!hasDragged && diff > touchSlop) {
                            hasDragged = true
                            isDragging = true
                        }
                        if (hasDragged) {
                            val rawOffset = currentX - (tabWidthPx / 2f)
                            dragOffsetPx = rawOffset.coerceIn(0f, (count - 1) * tabWidthPx)
                            val newIndex = (currentX / tabWidthPx).toInt().coerceIn(0, count - 1)
                            if (newIndex != activeIndex) {
                                activeIndex = newIndex
                                VibeHaptic.tab(view)
                                onItemSelected(newIndex)
                            }
                            change.consume()
                        }
                    }

                    if (hasDragged) {
                        isDragging = false
                    } else {
                        // Instant clean tap without drag
                        val tappedIndex = (startX / tabWidthPx).toInt().coerceIn(0, count - 1)
                        if (tappedIndex != selectedIndex) {
                            VibeHaptic.tab(view)
                            onItemSelected(tappedIndex)
                        }
                    }
                }
            }
    ) {
        val count = items.size.coerceAtLeast(1)
        val tabWidthDp = maxWidth / count

        val targetOffsetDp = if (isDragging) {
            with(density) { dragOffsetPx.toDp() }
        } else {
            tabWidthDp * selectedIndex.coerceIn(0, count - 1)
        }

        val indicatorOffset by animateDpAsState(
            targetValue = targetOffsetDp,
            animationSpec = if (isDragging) {
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            },
            label = "liquidGlassSliderOffset"
        )

        // Sliding Glass Indicator Capsule
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidthDp)
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
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (useVertical) {
                        // Symmetrical Vertical Layout: Icon on Top, Label on Bottom (No text truncation)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                item.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                if (item.count != null && item.count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-4).dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.3f) else Color(0xFF0071E3))
                                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                                    ) {
                                        Text(
                                            text = item.count.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Horizontal Layout: Icon and Label side-by-side
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
                            if (item.count != null && item.count > 0) {
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
                                        text = item.count.toString(),
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
}
