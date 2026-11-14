package com.videopoker.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HoldCatalogue
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    onOpenCompleteList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPracticeWait = (mode == GameMode.PRACTICE && dealState == DealState.DEALT_CHOOSING_HOLDS)
    val isAfterDraw = (dealState == DealState.SHOWDOWN_WIN || dealState == DealState.SHOWDOWN_NO_WIN)
    val scale = LocalResponsiveMetrics.current.uiScale
    val panelRadius = (8f * scale).dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(panelRadius))
            .background(palette.panelColor)
            .border(1.5.dp, palette.panelBorderColor, RoundedCornerShape(panelRadius))
            .padding(horizontal = (6f * scale).dp, vertical = (3f * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar: Title on left, Complete List button on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = (2f * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        mode == GameMode.PRACTICE && isPracticeWait -> "PRACTICE MODE"
                        mode == GameMode.PRACTICE -> "PRACTICE REVIEW"
                        else -> "STRATEGY ADVISOR"
                    },
                    color = palette.goldColor,
                    style = TextStyle(
                        fontSize = (11.5f * scale).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (0.8f * scale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                val btnRadius = (4f * scale).dp
                Box(
                    modifier = Modifier
                        .height((24f * scale).dp)
                        .clip(RoundedCornerShape(btnRadius))
                        .background(Color(0x22FFD700))
                        .border(1.dp, palette.goldColor.copy(alpha = 0.7f), RoundedCornerShape(btnRadius))
                        .clickable { onOpenCompleteList() }
                        .padding(horizontal = (8f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COMPLETE LIST",
                        color = palette.goldColor,
                        style = TextStyle(
                            fontSize = (9.5f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            if (isPracticeWait) {
                // Practice Mode before draw: Player decides on their own without hints
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = (4f * scale).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Select your cards to hold, then tap DRAW.\nOptimal strategy review will appear here after the draw.",
                        color = palette.payTableTextColor,
                        style = TextStyle(
                            fontSize = (11f * scale).sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = (15f * scale).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            } else if (choices.isEmpty()) {
                // Waiting for deal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Deal a hand to view optimal holds and strategy choices.",
                        color = palette.textDimColor,
                        style = TextStyle(
                            fontSize = (11f * scale).sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            } else {
                // Learn Mode OR Practice Mode after draw:
                // 2-row constrained viewport with vertical scrolling and scaled card chips
                val matchedIndex = if (isAfterDraw) {
                    choices.indexOfFirst { it.holdMask == playerHeldMask }
                } else -1
                val playerHeldOffStrategy = isAfterDraw && matchedIndex == -1
                val totalRowCount = choices.size + if (playerHeldOffStrategy) 1 else 0
                val listState = rememberLazyListState()

                LaunchedEffect(choices) {
                    listState.scrollToItem(0)
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val density = LocalDensity.current
                    val rowSpacing = (4f * scale).dp
                    val rowHeight = with(density) {
                        val availPx = constraints.maxHeight - rowSpacing.roundToPx()
                        (availPx / 2).toDp().coerceAtLeast(36.dp)
                    }
                    val rowScale = (rowHeight.value / 48f).coerceIn(0.95f, 1.95f)
                    val chipHeight = (rowHeight * 0.84f).coerceIn(32.dp, 92.dp)
                    val chipWidth = chipHeight * 0.72f
                    val snapFlingBehavior = rememberSnapFlingBehavior(
                        lazyListState = listState,
                        snapPosition = SnapPosition.Start
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyColumn(
                            state = listState,
                            flingBehavior = snapFlingBehavior,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(rowSpacing)
                        ) {
                            itemsIndexed(choices) { idx, choice ->
                                HoldChoiceRow(
                                    choice = choice,
                                    isTop = (idx == 0),
                                    isPlayerHold = (isAfterDraw && idx == matchedIndex),
                                    rowHeight = rowHeight,
                                    rowScale = rowScale,
                                    chipWidth = chipWidth,
                                    chipHeight = chipHeight,
                                    palette = palette
                                )
                            }

                            if (playerHeldOffStrategy) {
                                item {
                                    val heldCards = dealtCards.filterIndexed { i, _ ->
                                        playerHeldMask.getOrElse(i) { false }
                                    }
                                    OffStrategyHoldRow(
                                        heldCards = heldCards,
                                        rowHeight = rowHeight,
                                        rowScale = rowScale,
                                        chipWidth = chipWidth,
                                        chipHeight = chipHeight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width((4f * scale).dp))

                        HoldListScrollbar(
                            listState = listState,
                            totalItems = totalRowCount,
                            palette = palette,
                            modifier = Modifier
                                .width((10f * scale).dp)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldChoiceRow(
    choice: HoldChoice,
    isTop: Boolean,
    isPlayerHold: Boolean,
    rowHeight: Dp,
    rowScale: Float,
    chipWidth: Dp,
    chipHeight: Dp,
    palette: ThemePalette
) {
    val badgeColor = when (choice.row) {
        1 -> palette.choice1Color
        2 -> palette.choice2Color
        else -> palette.choice3Color
    }
    val badgeTextColor = if (badgeColor.luminance() < 0.4f) Color.White else Color(0xFF14100A)
    val rowRadius = (6f * rowScale).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(RoundedCornerShape(rowRadius))
            .background(if (isTop) palette.goldColor.copy(alpha = 0.14f) else Color(0x18FFFFFF))
            .border(
                width = if (isPlayerHold) 1.5.dp else 1.dp,
                color = when {
                    isPlayerHold -> Color(0xFF43C96C)
                    isTop -> palette.goldColor.copy(alpha = 0.55f)
                    else -> Color(0x28FFFFFF)
                },
                shape = RoundedCornerShape(rowRadius)
            )
            .padding(horizontal = (6f * rowScale).dp, vertical = (2f * rowScale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rank Badge (#1, #2, #3...)
        Box(
            modifier = Modifier
                .size((22f * rowScale).dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${choice.row}",
                color = badgeTextColor,
                style = TextStyle(
                    fontSize = (10.5f * rowScale).sp,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        Spacer(modifier = Modifier.width((6f * rowScale).dp))

        // 2. Enlarged Readable Card Chips
        HoldCardStrip(
            cards = choice.keptCards,
            rowScale = rowScale,
            chipWidth = chipWidth,
            chipHeight = chipHeight,
            pillTextColor = palette.payTableTextColor
        )

        // 3. Description & Single Priority Slot Number
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = (4f * rowScale).dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = choice.description,
                color = palette.payTableTextColor,
                style = TextStyle(
                    fontSize = (12f * rowScale).sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val keepsText = HoldCatalogue.getInfo(choice.ruleId).keeps
            val subText = if (choice.slot > 0) {
                if (keepsText.isNotBlank()) "Spot ${choice.slot}  •  $keepsText" else "Spot ${choice.slot}"
            } else {
                keepsText
            }
            if (subText.isNotBlank()) {
                Spacer(modifier = Modifier.height((1f * rowScale).dp))
                Text(
                    text = subText,
                    color = palette.textDimColor,
                    style = TextStyle(
                        fontSize = (10f * rowScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 4. Right Badges: Top Choice and Player Held Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy((4f * rowScale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTop) {
                HoldTagBadge(text = "✓ TOP", background = Color(0xFF2B8A3E), rowScale = rowScale)
            }
            if (isPlayerHold) {
                HoldTagBadge(
                    text = if (isTop) "PLAYED" else "HELD",
                    background = if (isTop) Color(0xFF1E7E34) else Color(0xFFC3162F),
                    rowScale = rowScale
                )
            }
        }
    }
}

@Composable
private fun OffStrategyHoldRow(
    heldCards: List<Card>,
    rowHeight: Dp,
    rowScale: Float,
    chipWidth: Dp,
    chipHeight: Dp
) {
    val rowRadius = (6f * rowScale).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(RoundedCornerShape(rowRadius))
            .background(Color(0x22C3162F))
            .border(1.5.dp, Color(0xFFC3162F), RoundedCornerShape(rowRadius))
            .padding(horizontal = (6f * rowScale).dp, vertical = (2f * rowScale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((22f * rowScale).dp)
                .clip(CircleShape)
                .background(Color(0xFFC3162F)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = Color.White,
                style = TextStyle(
                    fontSize = (10.5f * rowScale).sp,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        Spacer(modifier = Modifier.width((6f * rowScale).dp))

        HoldCardStrip(
            cards = heldCards,
            rowScale = rowScale,
            chipWidth = chipWidth,
            chipHeight = chipHeight,
            pillTextColor = Color.White
        )

        Text(
            text = "Your hold (Off-strategy)",
            color = Color(0xFFFFCDD2),
            style = TextStyle(
                fontSize = (11.5f * rowScale).sp,
                fontWeight = FontWeight.Bold,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = (4f * rowScale).dp)
        )

        HoldTagBadge(text = "HELD", background = Color(0xFFC3162F), rowScale = rowScale)
    }
}

@Composable
private fun HoldCardStrip(
    cards: List<Card>,
    rowScale: Float,
    chipWidth: Dp,
    chipHeight: Dp,
    pillTextColor: Color
) {
    Row(
        modifier = Modifier.padding(end = (6f * rowScale).dp),
        horizontalArrangement = Arrangement.spacedBy((3f * rowScale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cards.isEmpty()) {
            val pillRadius = (4f * rowScale).dp
            Box(
                modifier = Modifier
                    .height(chipHeight * 0.72f)
                    .clip(RoundedCornerShape(pillRadius))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(pillRadius))
                    .padding(horizontal = (8f * rowScale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DISCARD ALL",
                    color = pillTextColor,
                    style = TextStyle(
                        fontSize = (10.5f * rowScale).sp,
                        fontWeight = FontWeight.Bold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        } else {
            cards.forEach { card ->
                HoldCardChip(card = card, width = chipWidth, height = chipHeight)
            }
        }
    }
}

/**
 * High-contrast miniature playing card sized cleanly for the 2-row hold list viewport.
 */
@Composable
private fun HoldCardChip(
    card: Card,
    width: Dp,
    height: Dp
) {
    val isRed = (card.suit == 'h' || card.suit == 'd')
    val inkColor = if (isRed) Color(0xFFD32F2F) else Color(0xFF141414)
    val rankLabel = if (card.rank == 'T') "10" else card.rank.toString()
    val suitLabel = when (card.suit) {
        's' -> "♠"
        'h' -> "♥"
        'd' -> "♦"
        else -> "♣"
    }
    val rankFontSp = ((height.value * if (card.rank == 'T') 0.36f else 0.40f)).coerceIn(12f, 34f).sp
    val suitFontSp = (height.value * 0.36f).coerceIn(11f, 31f).sp

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = rankLabel,
                color = inkColor,
                style = TextStyle(
                    fontSize = rankFontSp,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Text(
                text = suitLabel,
                color = inkColor,
                style = TextStyle(
                    fontSize = suitFontSp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
private fun HoldTagBadge(text: String, background: Color, rowScale: Float = 1f) {
    val badgeRadius = (4f * rowScale).dp
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(badgeRadius))
            .background(background)
            .padding(horizontal = (6f * rowScale).dp, vertical = (2.5f * rowScale).dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = TextStyle(
                fontSize = (9f * rowScale).sp,
                fontWeight = FontWeight.Black,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

/**
 * Interactive vertical scrollbar synchronized with the 2-row hold list LazyColumn.
 */
@Composable
private fun HoldListScrollbar(
    listState: LazyListState,
    totalItems: Int,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val canScroll = totalItems > 2

    fun scrollToYFraction(yFraction: Float) {
        if (!canScroll) return
        val maxStart = (totalItems - 2).coerceAtLeast(0)
        val targetIndex = (yFraction.coerceIn(0f, 1f) * maxStart).roundToInt().coerceIn(0, maxStart)
        coroutineScope.launch {
            listState.scrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .pointerInput(totalItems) {
                detectTapGestures { offset ->
                    if (size.height > 0) {
                        scrollToYFraction(offset.y / size.height.toFloat())
                    }
                }
            }
            .pointerInput(totalItems) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    if (size.height > 0) {
                        scrollToYFraction(change.position.y / size.height.toFloat())
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = 4.dp.toPx()
            val trackLeft = (size.width - trackWidth) / 2f
            val corner = CornerRadius(trackWidth / 2f, trackWidth / 2f)

            drawRoundRect(
                color = Color(0x28FFFFFF),
                topLeft = Offset(trackLeft, 0f),
                size = Size(trackWidth, size.height),
                cornerRadius = corner
            )

            if (totalItems > 0) {
                val visibleCount = 2f
                val firstItemPx = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                val offsetFraction = (listState.firstVisibleItemScrollOffset / firstItemPx.coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
                val scrollableCount = (totalItems - visibleCount).coerceAtLeast(0f)
                val thumbFraction = if (canScroll) (visibleCount / totalItems.toFloat()).coerceIn(0.24f, 0.85f) else 1f
                val scrollProgress = if (scrollableCount > 0f) {
                    ((listState.firstVisibleItemIndex + offsetFraction) / scrollableCount).coerceIn(0f, 1f)
                } else 0f

                val thumbHeight = size.height * thumbFraction
                val thumbTop = (size.height - thumbHeight) * scrollProgress
                val thumbWidth = 6.dp.toPx()
                val thumbLeft = (size.width - thumbWidth) / 2f
                val thumbColor = if (canScroll) palette.goldColor.copy(alpha = 0.85f) else Color(0x44FFFFFF)

                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                )
            }
        }
    }
}
