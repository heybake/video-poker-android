package com.videopoker.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.R
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import kotlinx.coroutines.delay

data class SplashLayoutMetrics(
    val scale: Float,
    val borderInsetDp: Float,
    val heroFillFractionH: Float,
    val titleSizeSp: Float,
    val subtitleSizeSp: Float,
    val hintSizeSp: Float
)

fun calculateSplashLayoutMetrics(
    viewportWidthDp: Float,
    viewportHeightDp: Float
): SplashLayoutMetrics {
    val safeW = viewportWidthDp.coerceAtLeast(360f)
    val safeH = viewportHeightDp.coerceAtLeast(280f)
    val scale = (safeH / 411f).coerceIn(0.82f, 1.9f)
    val isTablet = safeW >= 1050f && safeH >= 550f

    return SplashLayoutMetrics(
        scale = scale,
        borderInsetDp = if (isTablet) 16f else 8f,
        heroFillFractionH = if (isTablet) 0.88f else 0.90f,
        titleSizeSp = (22f * scale).coerceIn(18f, 40f),
        subtitleSizeSp = (9.5f * scale).coerceIn(8f, 17f),
        hintSizeSp = (11f * scale).coerceIn(9.5f, 19f)
    )
}

@Composable
fun StartupSplashScreen(
    gamesReady: Boolean,
    appVersion: String,
    onDismiss: () -> Unit
) {
    val metrics = LocalResponsiveMetrics.current
    val splashMetrics = remember(metrics.viewportWidth, metrics.viewportHeight) {
        calculateSplashLayoutMetrics(
            viewportWidthDp = metrics.viewportWidth.value,
            viewportHeightDp = metrics.viewportHeight.value
        )
    }

    val entranceProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        )
    }

    // Hold for 2.8s so the player sees the bar-top scene over their live game, then fade out cleanly
    val minDisplayElapsed = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2800L)
        minDisplayElapsed.value = true
        delay(2200L)
        onDismiss()
    }
    LaunchedEffect(minDisplayElapsed.value, gamesReady) {
        if (minDisplayElapsed.value && gamesReady) {
            onDismiss()
        }
    }

    val goldAccent = Color(0xFFD4AF37)
    val brightChampagne = Color(0xFFFFF3D4)
    val subtleGold = Color(0xFFE5C478)
    val s = splashMetrics.scale

    // Transparent overlay so the live running game table shows right through behind the characters
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x22000000),
                        Color(0x4A000000),
                        Color(0x70000000)
                    )
                )
            )
            .semantics { contentDescription = "Startup splash screen" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        val inset = splashMetrics.borderInsetDp.dp

        // Subtle gold framing hairline around the live table viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .border(
                    width = (1.5f * s).coerceIn(1f, 2.5f).dp,
                    color = goldAccent.copy(alpha = 0.50f),
                    shape = RoundedCornerShape((12f * s).dp)
                )
        )

        // Transparent-background illustration anchored cleanly at bottom center
        Image(
            painter = painterResource(id = R.drawable.splash_hero_trio),
            contentDescription = "Royal Flush bar-top Video Poker characters",
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = inset)
                .fillMaxWidth()
                .fillMaxHeight(splashMetrics.heroFillFractionH)
                .graphicsLayer {
                    val t = entranceProgress.value
                    val zoom = 0.94f + 0.06f * t
                    scaleX = zoom
                    scaleY = zoom
                    alpha = t.coerceIn(0f, 1f)
                }
        )

        // Top-center compact crest pill sitting cleanly in the transparent sky area above the trio
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (splashMetrics.borderInsetDp + 8f * s).dp)
                .graphicsLayer { alpha = entranceProgress.value.coerceIn(0f, 1f) }
                .background(
                    color = Color(0xEB150508),
                    shape = RoundedCornerShape((20f * s).dp)
                )
                .border(
                    width = (1.25f * s).coerceIn(1f, 2f).dp,
                    color = goldAccent.copy(alpha = 0.72f),
                    shape = RoundedCornerShape((20f * s).dp)
                )
                .padding(horizontal = (18f * s).dp, vertical = (5f * s).dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((1f * s).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((8f * s).dp)
                ) {
                    Text(
                        text = "\u2660\uFE0E",
                        color = subtleGold,
                        style = TextStyle(
                            fontSize = (splashMetrics.titleSizeSp * 0.65f).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = "VIDEO POKER",
                        color = brightChampagne,
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = splashMetrics.titleSizeSp.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (2.5f * s).sp,
                            shadow = Shadow(
                                color = Color(0xDD000000),
                                offset = Offset(0f, 2f * s),
                                blurRadius = 5f * s
                            ),
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = "\u2660\uFE0E",
                        color = subtleGold,
                        style = TextStyle(
                            fontSize = (splashMetrics.titleSizeSp * 0.65f).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Text(
                    text = "STRATEGY TRAINER  •  41 CLASSIC GAMES",
                    color = subtleGold,
                    style = TextStyle(
                        fontSize = splashMetrics.subtitleSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1.6f * s).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        // Bottom prompt tucked crisply over the dark bar edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (splashMetrics.borderInsetDp + 8f * s).dp)
                .background(
                    color = Color(0xB80F0507),
                    shape = RoundedCornerShape((14f * s).dp)
                )
                .border(
                    width = 0.75.dp,
                    color = goldAccent.copy(alpha = 0.45f),
                    shape = RoundedCornerShape((14f * s).dp)
                )
                .padding(horizontal = (14f * s).dp, vertical = (4f * s).dp)
        ) {
            Text(
                text = "Tap anywhere to deal  •  v$appVersion",
                color = brightChampagne.copy(alpha = 0.90f),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = splashMetrics.hintSizeSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (0.7f * s).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}
