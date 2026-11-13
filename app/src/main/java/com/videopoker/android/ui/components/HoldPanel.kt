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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun HoldPanel(
    choices: List<HoldChoice>,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panelColor)
            .border(1.5.dp, palette.panelBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            choices.take(3).forEach { choice ->
                val badgeColor = when (choice.row) {
                    1 -> palette.choice1Color
                    2 -> palette.choice2Color
                    else -> palette.choice3Color
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color(0x15FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Number Badge (#1, #2, #3)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(badgeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${choice.row}",
                            color = Color(0xFF1A1A1A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Mini Card Chips (23dp x 16.5dp)
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (choice.keptCards.isEmpty()) {
                            Text(
                                text = "—",
                                color = palette.textDimColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                                            .width(23.dp)
                                            .height(16.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Keep Description Text
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = choice.description,
                                color = palette.textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (choice.isTopChoice) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF2B8A3E))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "✓ Top choice",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Spot numbers
                        val spotStr = if (choice.keptIndexes.isEmpty()) "None" else choice.keptIndexes.map { it + 1 }.joinToString(", ", "[", "]")
                        Text(
                            text = "Spots: $spotStr",
                            color = palette.textDimColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Expected Value
                    Text(
                        text = if (choice.expectedValue > 0.0) String.format("%.3f", choice.expectedValue) else "",
                        color = palette.goldColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
