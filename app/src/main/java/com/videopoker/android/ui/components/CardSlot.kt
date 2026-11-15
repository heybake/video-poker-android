package com.videopoker.android.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.Card
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.theme.CardDeck
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun CardSlot(
    card: Card?,
    isHeld: Boolean,
    cardIndex: Int,
    choicesHoldingCard: List<Int>, // 1-based choices holding this card (e.g. [1, 2])
    mode: GameMode,
    palette: ThemePalette,
    cardDeck: CardDeck = CardDeck.MODERN,
    cardWidth: Dp = 100.dp,
    cardHeight: Dp = 140.dp,
    onToggleHold: () -> Unit = {}
) {
    val context = LocalContext.current
    val liftOffset by animateDpAsState(targetValue = 0.dp, label = "cardLift")
    val cardScale = (cardWidth.value / 100f).coerceIn(0.6f, 2.6f)
    val cardRadius = (8f * cardScale).dp

    // Load card image from selected deck assets folder
    val cardBitmap = remember(card, cardDeck) {
        val fileName = card?.assetFileName ?: "back.png"
        try {
            context.assets.open("${cardDeck.assetFolder}/$fileName").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .offset(y = liftOffset),
        contentAlignment = Alignment.Center
    ) {
        // Card Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = if (isHeld) (10f * cardScale).dp else (4f * cardScale).dp, shape = RoundedCornerShape(cardRadius))
                .clip(RoundedCornerShape(cardRadius))
                .background(Color.White)
                .then(
                    if (isHeld) {
                        Modifier.border((2.5f * cardScale).coerceAtLeast(2f).dp, palette.goldColor, RoundedCornerShape(cardRadius))
                    } else {
                        Modifier.border(1.dp, Color(0xFF444444), RoundedCornerShape(cardRadius))
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

        // Authentic Theme-Adhering HELD Badge
        if (isHeld) {
            val heldScale = cardScale.coerceIn(0.75f, 1.55f)
            val heldRadius = (4f * heldScale).dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8f * heldScale).dp)
                    .shadow(elevation = (4f * heldScale).dp, shape = RoundedCornerShape(heldRadius))
                    .clip(RoundedCornerShape(heldRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.heldTagBgStart, palette.heldTagBgEnd)
                        )
                    )
                    .border((1.2f * heldScale).coerceAtLeast(1f).dp, palette.heldTagBorder, RoundedCornerShape(heldRadius))
                    .padding(horizontal = (10f * heldScale).dp, vertical = (2f * heldScale).dp)
            ) {
                Text(
                    text = "HELD",
                    color = palette.heldTagTextColor,
                    style = TextStyle(
                        fontSize = (11f * heldScale).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (1f * heldScale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        // Learn Mode Choice Dots (bottom-right corner)
        if (choicesHoldingCard.isNotEmpty()) {
            val dotSize = (10f * cardScale).coerceIn(8f, 20f).dp
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding((4f * cardScale).dp),
                horizontalArrangement = Arrangement.spacedBy((3f * cardScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                choicesHoldingCard.forEach { choiceNum ->
                    val dotColor = when (choiceNum) {
                        1 -> palette.choice1Color
                        2 -> palette.choice2Color
                        else -> palette.choice3Color
                    }
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(1.dp, Color(0xFF222222), CircleShape)
                    )
                }
            }
        }
    }
}
