package com.videopoker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun HandPlaque(
    handRank: HandRank,
    palette: ThemePalette
) {
    val scale = LocalResponsiveMetrics.current.uiScale
    val plaqueHeight = (24f * scale).dp

    if (handRank == HandRank.NOTHING || handRank.displayName.isEmpty()) {
        Box(modifier = Modifier.height(plaqueHeight))
        return
    }

    val radius = (6f * scale).dp
    Box(
        modifier = Modifier
            .height(plaqueHeight)
            .shadow(elevation = (6f * scale).dp, shape = RoundedCornerShape(radius))
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(palette.plaqueBgStart, palette.plaqueBgEnd)
                )
            )
            .border((1.5f * scale).coerceAtLeast(1.2f).dp, palette.plaqueBorder, RoundedCornerShape(radius))
            .padding(horizontal = (24f * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = handRank.displayName,
            color = palette.plaqueTextColor,
            style = TextStyle(
                fontSize = (13f * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (1.2f * scale).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
