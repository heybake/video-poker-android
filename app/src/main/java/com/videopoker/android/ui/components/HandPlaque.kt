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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun HandPlaque(
    handRank: HandRank,
    palette: ThemePalette
) {
    if (handRank == HandRank.NOTHING || handRank.displayName.isEmpty()) {
        Box(modifier = Modifier.height(28.dp))
        return
    }

    Box(
        modifier = Modifier
            .height(28.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.plaqueBgStart, palette.plaqueBgEnd)
                )
            )
            .border(1.5.dp, palette.plaqueBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = handRank.displayName,
            color = palette.plaqueTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
}
