package com.vibeload.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Design System for VibeLoad.
 * Inspired by Apple VisionOS, iOS Liquid Glass, and Framer Liquid Glass aesthetics.
 * Features:
 * - Frosted glass surfaces with high-clarity translucency
 * - Specular glossy highlights (directional light reflection at top-left edge)
 * - Upper glossy refraction sheen simulating thick curved fluid glass
 * - Vibrant green and blue ambient lighting palette with deep contrast
 * - Soft ambient colored glow shadows
 */
object LiquidGlassTokens {
    // Vibrant Green & Blue Ambient Lighting Palette
    val GlassBlue = Color(0xFF0071E3)
    val GlassCyan = Color(0xFF00C6FF)
    val GlassElectricBlue = Color(0xFF0066FF)
    val GlassEmerald = Color(0xFF059669)
    val GlassMint = Color(0xFF10B981)
    val GlassTeal = Color(0xFF34D399)
    val GlassAmber = Color(0xFFF59E0B)
    val GlassCoral = Color(0xFFEA580C)

    // Primary Liquid Glass Gradients
    val LiquidBlueCyanBrush = Brush.linearGradient(
        colors = listOf(GlassElectricBlue, GlassBlue, GlassCyan),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val LiquidEmeraldMintBrush = Brush.linearGradient(
        colors = listOf(GlassEmerald, GlassMint, GlassTeal),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val LiquidAmberCoralBrush = Brush.linearGradient(
        colors = listOf(GlassAmber, GlassCoral),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Glass Background Colors
    fun glassSurface(isDark: Boolean, alpha: Float = 0.75f): Color {
        return if (isDark) {
            Color(0xFF121B2D).copy(alpha = alpha)
        } else {
            Color(0xFFFFFFFF).copy(alpha = if (alpha > 0.85f) alpha else 0.85f)
        }
    }

    fun glassElevated(isDark: Boolean, alpha: Float = 0.85f): Color {
        return if (isDark) {
            Color(0xFF18233A).copy(alpha = alpha)
        } else {
            Color(0xFFFFFFFF).copy(alpha = 0.95f)
        }
    }

    // Specular Highlight Border Brush (Directional 135-deg light reflection)
    fun specularBorder(isDark: Boolean, startAlpha: Float = if (isDark) 0.35f else 0.90f): Brush {
        return if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = startAlpha),
                    Color.White.copy(alpha = startAlpha * 0.35f),
                    Color.White.copy(alpha = 0.03f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = startAlpha),
                    Color.White.copy(alpha = startAlpha * 0.60f),
                    Color(0xFFCBD5E1).copy(alpha = 0.50f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }

    // Glossy Sheen Overlay Brush (Upper 45-50% specular reflection)
    fun glossySheenBrush(peakAlpha: Float = 0.38f): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = peakAlpha),
                Color.White.copy(alpha = peakAlpha * 0.20f),
                Color.Transparent
            )
        )
    }
}

/**
 * Ambient background lighting that projects soft glowing orbs of vibrant blue, cyan, and emerald green
 * behind the screen content, allowing frosted glass cards to refract real colors and achieve true depth.
 */
@Composable
fun LiquidGlassAmbientBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDark) Color(0xFF0A0F1D) else Color(0xFFF1F5F9)
            )
    ) {
        // Ambient Glow 1: Top-left Electric Azure / Cyan Glow
        val glow1Alpha = if (isDark) 0.16f else 0.12f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0071E3).copy(alpha = glow1Alpha),
                            Color(0xFF00C6FF).copy(alpha = glow1Alpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(200f, 250f),
                        radius = 800f
                    )
                )
        )

        // Ambient Glow 2: Center-Right Vibrant Mint / Emerald Glow
        val glow2Alpha = if (isDark) 0.12f else 0.08f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = glow2Alpha),
                            Color(0xFF34D399).copy(alpha = glow2Alpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(1100f, 1300f),
                        radius = 900f
                    )
                )
        )

        // Ambient Glow 3: Bottom-Left Soft Indigo Glow
        val glow3Alpha = if (isDark) 0.10f else 0.06f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = glow3Alpha),
                            Color.Transparent
                        ),
                        center = Offset(300f, 2200f),
                        radius = 750f
                    )
                )
        )

        // Content
        content()
    }
}

/**
 * Reusable Frosted Glass Container with specular highlight border and top glossy sheen.
 */
@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    spotGlowColor: Color = Color(0xFF0071E3).copy(alpha = 0.25f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = spotGlowColor,
                ambientColor = Color(0xFF10B981).copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(LiquidGlassTokens.glassSurface(isDark))
            .border(
                width = 1.dp,
                brush = LiquidGlassTokens.specularBorder(isDark),
                shape = shape
            )
    ) {
        // Upper Glossy Sheen Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(LiquidGlassTokens.glossySheenBrush(if (isDark) 0.15f else 0.35f))
        )

        content()
    }
}
