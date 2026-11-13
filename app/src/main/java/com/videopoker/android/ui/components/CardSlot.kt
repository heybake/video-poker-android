package com.videopoker.android.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.Card
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun CardSlot(
    card: Card?,
    isHeld: Boolean,
    cardIndex: Int,
    choicesHoldingCard: List<Int>, // 1-based choices holding this card (e.g. [1, 2])
    mode: GameMode,
    palette: ThemePalette,
    onToggleHold: () -> Unit
) {
    val context = LocalContext.current
    val liftOffset by animateDpAsState(targetValue = if (isHeld) (-5).dp else 0.dp, label = "cardLift")

    // Load card image from assets
    val cardBitmap = remember(card) {
        val fileName = card?.assetFileName ?: "back.png"
        try {
            context.assets.open("cards/$fileName").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .width(96.dp)
            .height(134.dp)
            .offset(y = liftOffset)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleHold
            ),
        contentAlignment = Alignment.Center
    ) {
        // Card Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = if (isHeld) 10.dp else 4.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .then(
                    if (isHeld) {
                        Modifier.border(2.5.dp, palette.goldColor, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                    }
                )
        ) {
            if (cardBitmap != null) {
                Image(
                    bitmap = cardBitmap,
                    contentDescription = card?.toString() ?: "Card",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // HELD Badge
        if (isHeld) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.goldColor, palette.panelBorderSecondary)
                        )
                    )
                    .border(1.dp, Color(0xFFFFF0D0), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "HELD",
                    color = palette.buttonTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        // Learn Mode Choice Dots (bottom-right corner)
        if (mode == GameMode.LEARN && choicesHoldingCard.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (1 in choicesHoldingCard) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.choice1Color)
                    )
                }
                if (2 in choicesHoldingCard) {
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.choice2Color)
                    )
                }
                if (3 in choicesHoldingCard) {
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.choice3Color)
                    )
                }
            }
        }
    }
}
