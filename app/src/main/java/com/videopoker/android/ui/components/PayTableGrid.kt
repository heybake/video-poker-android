package com.videopoker.android.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panelColor)
            .border(1.5.dp, palette.panelBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = if (isCasino) 10.dp else 8.dp, vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            rows.forEach { row ->
                val isWinRow = (winningRank != null && row.key == winningRank.key)

                if (isCasino) {
                    // Full 6-column row in Casino Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hand Name
                        Text(
                            text = row.label,
                            color = if (isWinRow) palette.winRowBg else palette.payTableTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // 5 Coin Columns
                        for (coin in 1..5) {
                            val isCurrentBetCol = (coin == currentBet)
                            val isWinningCell = (isWinRow && isCurrentBetCol)
                            val cellVal = row.values.getOrElse(coin - 1) { 0 }

                            Box(
                                modifier = Modifier
                                    .weight(0.35f)
                                    .fillMaxHeight()
                                    .then(
                                        if (isWinningCell) {
                                            Modifier.background(palette.winRowBg, RoundedCornerShape(2.dp))
                                        } else if (isCurrentBetCol) {
                                            Modifier.background(Color(0x22C9A24D))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = cellVal.toString(),
                                    color = if (isWinningCell) palette.winRowText else if (isCurrentBetCol) palette.goldColor else palette.payTableTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrentBetCol) FontWeight.ExtraBold else FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                } else {
                    // Single-column row with dotted leader in Learn / Practice Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.2.dp)
                            .then(
                                if (isWinRow) {
                                    Modifier.background(palette.winRowBg, RoundedCornerShape(2.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.label,
                            color = if (isWinRow) palette.winRowText else palette.payTableTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Dotted leader line
                        val dotColor = palette.payTableGridColor
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .padding(horizontal = 6.dp)
                                .drawBehind {
                                    if (!isWinRow) {
                                        drawLine(
                                            color = dotColor,
                                            start = Offset(0f, size.height / 2),
                                            end = Offset(size.width, size.height / 2),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                        )
                                    }
                                }
                        )

                        // Value
                        val valStr = row.values.getOrElse(currentBet - 1) { 0 }.toString()
                        Text(
                            text = valStr,
                            color = if (isWinRow) palette.winRowText else palette.payTableTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
