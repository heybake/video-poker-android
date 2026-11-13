package com.videopoker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun PayTableGrid(
    game: GameConfig?,
    currentBet: Int,
    winningRank: HandRank?,
    mode: GameMode,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val rows = game?.payTable?.rows ?: emptyList()
    val isCasino = (mode == GameMode.CASINO)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panelColor)
            .border(1.5.dp, palette.panelBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = if (isCasino) 8.dp else 6.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            rows.forEach { row ->
                val isWinRow = (winningRank != null && row.key == winningRank.key)

                if (isCasino) {
                    // Full 6-column row in Casino Mode (Hand name + dots stretching far across + 5 coin values)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .then(
                                if (isWinRow) Modifier.background(palette.winRowBg, RoundedCornerShape(2.dp))
                                else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hand Name with Dotted Leader extending across ~65% of the pay box
                        Row(
                            modifier = Modifier
                                .weight(2.3f)
                                .padding(start = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.label,
                                color = if (isWinRow) palette.winRowText else palette.payTableTextColor,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Dotted leader line extending right up to the 1st coin column
                            val dotColor = palette.payTableGridColor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .padding(start = 4.dp, end = 2.dp)
                                    .drawBehind {
                                        if (!isWinRow) {
                                            drawLine(
                                                color = dotColor,
                                                start = Offset(0f, size.height / 2),
                                                end = Offset(size.width, size.height / 2),
                                                strokeWidth = 1.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                                            )
                                        }
                                    }
                            )
                        }

                        // 5 Coin Columns (tightly packed on right so dots go far across)
                        for (coin in 1..5) {
                            val isCurrentBetCol = (coin == currentBet)
                            val isWinningCell = (isWinRow && isCurrentBetCol)
                            val cellVal = row.values.getOrElse(coin - 1) { 0 }

                            Box(
                                modifier = Modifier
                                    .weight(0.25f)
                                    .fillMaxHeight()
                                    .then(
                                        if (isWinningCell) {
                                            Modifier.background(Color(0xFFE5B800), RoundedCornerShape(2.dp))
                                        } else if (isCurrentBetCol) {
                                            Modifier.background(palette.payTableGridColor, RoundedCornerShape(2.dp))
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$cellVal",
                                    color = when {
                                        isWinningCell -> Color(0xFF111111)
                                        isWinRow -> palette.winRowText
                                        isCurrentBetCol -> palette.goldColor
                                        else -> palette.payTableTextColor
                                    },
                                    style = TextStyle(
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isCurrentBetCol || isWinRow) FontWeight.Black else FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Split mode (Learn & Practice): Hand name on left, current bet payout on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .then(
                                if (isWinRow) Modifier.background(palette.winRowBg, RoundedCornerShape(2.dp))
                                else Modifier
                            )
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.label,
                            color = if (isWinRow) palette.winRowText else palette.payTableTextColor,
                            style = TextStyle(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val payout = row.values.getOrElse(currentBet - 1) { 0 }
                        Text(
                            text = "$payout",
                            color = if (isWinRow) palette.winRowText else palette.goldColor,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.End,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        }
    }
}
