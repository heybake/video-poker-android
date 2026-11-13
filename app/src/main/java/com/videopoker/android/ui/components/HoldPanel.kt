package com.videopoker.android.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun HoldPanel(
    choices: List<HoldChoice>,
    playerHeldMask: List<Boolean>,
    dealtCards: List<Card>,
    mode: GameMode,
    dealState: DealState,
    practiceFeedback: String?,
    practiceMadeTopChoice: Boolean?,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPracticeWait = (mode == GameMode.PRACTICE && dealState == DealState.DEALT_CHOOSING_HOLDS)
    val isAfterDraw = (dealState == DealState.SHOWDOWN_WIN || dealState == DealState.SHOWDOWN_NO_WIN)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panelColor)
            .border(1.5.dp, palette.panelBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isPracticeWait) {
            // Practice Mode before draw: Player decides on their own without hints
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PRACTICE MODE",
                    color = palette.goldColor,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select your cards to hold, then tap DRAW.\nOptimal strategy review will appear here after the draw.",
                    color = palette.payTableTextColor,
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        } else if (choices.isEmpty()) {
            // Waiting for deal
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (mode == GameMode.LEARN) "STRATEGY ADVISOR" else "PRACTICE REVIEW",
                    color = palette.goldColor,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deal a hand to view optimal holds and strategy choices.",
                    color = palette.textDimColor,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        } else {
            // Learn Mode OR Practice Mode after draw: Show Hold Choices cleanly
            val topChoices = choices.take(3)
            val matchedIndex = if (isAfterDraw) {
                topChoices.indexOfFirst { it.holdMask == playerHeldMask }
            } else -1
            val playerHeldOffStrategy = isAfterDraw && matchedIndex == -1

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                topChoices.forEachIndexed { idx, choice ->
                    val isTop = (idx == 0)
                    val isPlayerHold = (isAfterDraw && idx == matchedIndex)
                    val badgeColor = when (choice.row) {
                        1 -> palette.choice1Color
                        2 -> palette.choice2Color
                        else -> palette.choice3Color
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 1.5.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isTop) palette.goldColor.copy(alpha = 0.14f) else Color(0x18FFFFFF)
                            )
                            .border(
                                width = if (isPlayerHold) 1.5.dp else 1.dp,
                                color = if (isPlayerHold) Color(0xFF43C96C)
                                else if (isTop) palette.goldColor.copy(alpha = 0.55f)
                                else Color(0x28FFFFFF),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Rank Badge (#1, #2, #3)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${choice.row}",
                                color = Color(0xFF14100A),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 2. Large Readable Card Chips (27dp x 38dp)
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (choice.keptCards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33FFFFFF))
                                        .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DISCARD ALL",
                                        color = palette.payTableTextColor,
                                        style = TextStyle(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            } else {
                                choice.keptCards.forEach { card ->
                                    val chipBitmap = remember(card) {
                                        try {
                                            context.assets.open("cards/${card.assetFileName}").use { stream ->
                                                BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (chipBitmap != null) {
                                        Image(
                                            bitmap = chipBitmap,
                                            contentDescription = card.toString(),
                                            modifier = Modifier
                                                .width(27.dp)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .border(0.5.dp, Color(0x44000000), RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Description (No confusing table slot numbers)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = choice.description,
                                color = palette.payTableTextColor,
                                style = TextStyle(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 4. Badges on the right: ONLY Top Choice and You Held This
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTop) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2B8A3E))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "✓ TOP CHOICE",
                                        color = Color.White,
                                        style = TextStyle(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }

                            if (isPlayerHold) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isTop) Color(0xFF1E7E34) else Color(0xFFC3162F)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isTop) "YOU PLAYED THIS" else "YOU HELD THIS",
                                        color = Color.White,
                                        style = TextStyle(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // If player held an off-strategy combination not on the top list
                if (playerHeldOffStrategy) {
                    val heldCards = dealtCards.filterIndexed { i, _ -> playerHeldMask.getOrElse(i) { false } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 1.5.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22C3162F))
                            .border(1.5.dp, Color(0xFFC3162F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC3162F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (heldCards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33FFFFFF))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DISCARD ALL",
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                heldCards.forEach { card ->
                                    val chipBitmap = remember(card) {
                                        try {
                                            context.assets.open("cards/${card.assetFileName}").use { stream ->
                                                BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (chipBitmap != null) {
                                        Image(
                                            bitmap = chipBitmap,
                                            contentDescription = card.toString(),
                                            modifier = Modifier
                                                .width(27.dp)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Your hold (Off-strategy)",
                            color = Color(0xFFFFCDD2),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFC3162F))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "YOU HELD THIS",
                                color = Color.White,
                                style = TextStyle(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
