package com.videopoker.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.ui.state.BankrollPoint
import com.videopoker.android.ui.state.BankrollTargets
import com.videopoker.android.ui.state.SessionBankrollTracker
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SessionBankrollPanel(
    points: List<BankrollPoint>,
    targets: BankrollTargets,
    cashBalance: Double,
    palette: ThemePalette,
    showCompleteListButton: Boolean = false,
    onOpenCompleteList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scale = LocalResponsiveMetrics.current.uiScale
    val panelRadius = (8f * scale).dp
    val isLightPanel = palette.panelColor.luminance() > 0.45f

    val balanceColor = if (isLightPanel) Color(0xFF0369A1) else Color(0xFF38BDF8)
    val buyInColor = if (isLightPanel) Color(0xFF64748B) else Color(0xFFCBD5E1)
    val winGoalColor = if (isLightPanel) Color(0xFF15803D) else Color(0xFF4ADE80)
    val stopLossColor = if (isLightPanel) Color(0xFFB91C1C) else Color(0xFFF87171)
    val suboptimalDotColor = Color(0xFFEF4444)

    val latestHand = points.lastOrNull()?.handNumber ?: 0
    var requestedEndHand by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(latestHand) {
        requestedEndHand = null
    }

    val viewport = remember(points, targets, requestedEndHand) {
        SessionBankrollTracker.calculateViewport(
            points = points,
            targets = targets,
            requestedEndHand = requestedEndHand
        )
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(panelRadius))
            .background(palette.panelColor)
            .border(1.5.dp, palette.panelBorderColor, RoundedCornerShape(panelRadius))
            .padding(horizontal = (6f * scale).dp, vertical = (3f * scale).dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Title, compact inline legend, optional LIVE snap pill & COMPLETE LIST button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = (2f * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SESSION BANKROLL",
                    color = palette.goldColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontSize = (11.0f * scale).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (0.6f * scale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((5f * scale).dp)
                    ) {
                        ChartLegendItem(label = "Bal", color = balanceColor, isDashed = false, isDot = false, scale = scale, textColor = palette.payTableTextColor)
                        ChartLegendItem(label = "In", color = buyInColor, isDashed = false, isDot = false, scale = scale, textColor = palette.payTableTextColor)
                        if (targets.winGoalEnabled) {
                            ChartLegendItem(label = "Goal", color = winGoalColor, isDashed = true, isDot = false, scale = scale, textColor = palette.payTableTextColor)
                        }
                        if (targets.stopLossEnabled) {
                            ChartLegendItem(label = "Stop", color = stopLossColor, isDashed = true, isDot = false, scale = scale, textColor = palette.payTableTextColor)
                        }
                        ChartLegendItem(label = "Err", color = suboptimalDotColor, isDashed = false, isDot = true, scale = scale, textColor = palette.payTableTextColor)
                    }

                    if (viewport.canPan && !viewport.isLiveEdge) {
                        val liveRadius = (4f * scale).dp
                        Box(
                            modifier = Modifier
                                .height((20f * scale).dp)
                                .clip(RoundedCornerShape(liveRadius))
                                .background(palette.goldColor)
                                .clickable { requestedEndHand = null }
                                .padding(horizontal = (6f * scale).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LIVE ▸",
                                color = Color(0xFF14100A),
                                style = TextStyle(
                                    fontSize = (8.5f * scale).sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }

                    if (showCompleteListButton) {
                        val btnRadius = (4f * scale).dp
                        Box(
                            modifier = Modifier
                                .height((22f * scale).dp)
                                .clip(RoundedCornerShape(btnRadius))
                                .background(Color(0x22FFD700))
                                .border(1.dp, palette.goldColor.copy(alpha = 0.7f), RoundedCornerShape(btnRadius))
                                .clickable { onOpenCompleteList() }
                                .padding(horizontal = (7f * scale).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "COMPLETE LIST",
                                color = palette.goldColor,
                                style = TextStyle(
                                    fontSize = (9.0f * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    } else if (points.isNotEmpty()) {
                        Text(
                            text = String.format(Locale.US, "$%.2f", cashBalance),
                            color = palette.goldColor,
                            style = TextStyle(
                                fontSize = (10.5f * scale).sp,
                                fontWeight = FontWeight.Black,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            if (points.isEmpty()) {
                val targetSummary = buildList {
                    if (targets.winGoalEnabled) add("Win Goal +${targets.winGoalPercent}%")
                    if (targets.stopLossEnabled) add("Stop-Loss -${targets.stopLossPercent}%")
                }.joinToString("  •  ").ifEmpty { "Targets disabled" }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = (8f * scale).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Insert cash via INSERT CASH to track your session bankroll from Hand 0.",
                        color = palette.payTableTextColor,
                        style = TextStyle(
                            fontSize = (10.5f * scale).sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = targetSummary,
                        color = palette.textDimColor,
                        modifier = Modifier.padding(top = (3f * scale).dp),
                        style = TextStyle(
                            fontSize = (9.5f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            } else {
                val axisLabelStyle = TextStyle(
                    color = palette.textDimColor,
                    fontSize = (8.5f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
                val gridColor = if (isLightPanel) Color(0x28000000) else Color(0x28FFFFFF)
                val axisLineColor = if (isLightPanel) Color(0x55000000) else Color(0x55FFFFFF)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(viewport.canPan, viewport.maxHand, viewport.windowHands) {
                            if (!viewport.canPan) return@pointerInput
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val approxPlotW = (size.width * 0.85f).coerceAtLeast(100f)
                                val pxPerHand = approxPlotW / viewport.windowHands.toFloat()
                                val deltaHands = -dragAmount / pxPerHand
                                val curEnd = requestedEndHand ?: viewport.maxHand.toFloat()
                                requestedEndHand = (curEnd + deltaHands).coerceIn(
                                    viewport.windowHands.toFloat(),
                                    viewport.maxHand.toFloat()
                                )
                            }
                        }
                ) {
                    val yTopStr = SessionBankrollTracker.formatDollarAxis(viewport.maxY)
                    val yMidVal = (viewport.minY + viewport.maxY) * 0.5
                    val yMidStr = SessionBankrollTracker.formatDollarAxis(yMidVal)
                    val yBotStr = SessionBankrollTracker.formatDollarAxis(viewport.minY)

                    val yTopLayout = textMeasurer.measure(yTopStr, axisLabelStyle)
                    val yMidLayout = textMeasurer.measure(yMidStr, axisLabelStyle)
                    val yBotLayout = textMeasurer.measure(yBotStr, axisLabelStyle)

                    val maxYLabelW = max(yTopLayout.size.width, max(yMidLayout.size.width, yBotLayout.size.width))
                    val xSampleLayout = textMeasurer.measure("0", axisLabelStyle)

                    val plotLeft = maxYLabelW + (5f * scale).dp.toPx()
                    val plotRight = size.width - (5f * scale).dp.toPx()
                    val plotTop = (4f * scale).dp.toPx()
                    val plotBottom = size.height - xSampleLayout.size.height - (4f * scale).dp.toPx()
                    val plotW = (plotRight - plotLeft).coerceAtLeast(10f)
                    val plotH = (plotBottom - plotTop).coerceAtLeast(10f)

                    fun mapX(hand: Float): Float =
                        plotLeft + ((hand - viewport.startHand) / viewport.windowHands.toFloat()) * plotW

                    fun mapY(dollars: Double): Float {
                        val frac = ((dollars - viewport.minY) / (viewport.maxY - viewport.minY)).toFloat()
                        return plotBottom - (frac * plotH)
                    }

                    // Horizontal Y grid lines & Y-axis labels
                    val yTicks = listOf(
                        Triple(viewport.maxY, yTopLayout, plotTop),
                        Triple(yMidVal, yMidLayout, (plotTop + plotBottom) * 0.5f),
                        Triple(viewport.minY, yBotLayout, plotBottom)
                    )
                    yTicks.forEach { (_, layout, yPos) ->
                        drawLine(
                            color = gridColor,
                            start = Offset(plotLeft, yPos),
                            end = Offset(plotRight, yPos),
                            strokeWidth = 1.dp.toPx()
                        )
                        val labelY = (yPos - layout.size.height * 0.5f).coerceIn(0f, plotBottom - layout.size.height)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(plotLeft - layout.size.width - (3f * scale).dp.toPx(), labelY)
                        )
                    }

                    // Axes frame lines
                    drawLine(axisLineColor, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), strokeWidth = 1.dp.toPx())
                    drawLine(axisLineColor, Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), strokeWidth = 1.dp.toPx())

                    // X-axis hand ticks (every 5 hands)
                    val firstTick = ((ceil(viewport.startHand / 5f).toInt()) * 5).coerceAtLeast(0)
                    val lastTick = (viewport.endHand.toInt() / 5) * 5
                    for (tickHand in firstTick..lastTick step 5) {
                        val tx = mapX(tickHand.toFloat())
                        if (tx < plotLeft - 2f || tx > plotRight + 2f) continue
                        drawLine(
                            color = gridColor,
                            start = Offset(tx, plotTop),
                            end = Offset(tx, plotBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        val tickLayout = textMeasurer.measure("$tickHand", axisLabelStyle)
                        val labelX = (tx - tickLayout.size.width * 0.5f)
                            .coerceIn(plotLeft, plotRight - tickLayout.size.width)
                        drawText(
                            textLayoutResult = tickLayout,
                            topLeft = Offset(labelX, plotBottom + (2f * scale).dp.toPx())
                        )
                    }

                    val clipPad = (5f * scale).dp.toPx()
                    clipRect(
                        left = plotLeft - clipPad,
                        top = 0f,
                        right = plotRight + clipPad,
                        bottom = plotBottom + clipPad
                    ) {
                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf((6f * scale).dp.toPx(), (4f * scale).dp.toPx()),
                            0f
                        )
                        val stepStrokePx = (1.6f * scale).dp.toPx()
                        val targetStrokePx = (1.5f * scale).dp.toPx()
                        val balanceStrokePx = (2.2f * scale).dp.toPx()

                        fun buildBuyInDerivedPath(extendToRightEdge: Boolean, transform: (Double) -> Double): Path {
                            val path = Path()
                            points.forEachIndexed { idx, pt ->
                                val px = mapX(pt.handNumber.toFloat())
                                if (idx == 0) {
                                    path.moveTo(px, mapY(transform(pt.totalBuyInDollars)))
                                } else {
                                    val beforeBuyIn = pt.buyInBeforeInsert
                                    if (beforeBuyIn != null) {
                                        path.lineTo(px, mapY(transform(beforeBuyIn)))
                                        path.lineTo(px, mapY(transform(pt.totalBuyInDollars)))
                                    } else {
                                        path.lineTo(px, mapY(transform(pt.totalBuyInDollars)))
                                    }
                                }
                            }
                            if (extendToRightEdge && points.isNotEmpty()) {
                                val lastTargetY = mapY(transform(points.last().totalBuyInDollars))
                                path.lineTo(max(plotRight, mapX(points.last().handNumber.toFloat())), lastTargetY)
                            }
                            return path
                        }

                        // Series 4: Stop-Loss (Red Dashed Step Line)
                        if (targets.stopLossEnabled) {
                            val stopPath = buildBuyInDerivedPath(extendToRightEdge = true) {
                                targets.stopLossDollars(it)
                            }
                            drawPath(
                                path = stopPath,
                                color = stopLossColor,
                                style = Stroke(width = targetStrokePx, pathEffect = dashEffect, cap = StrokeCap.Round)
                            )
                        }

                        // Series 3: Win Goal (Green Dashed Step Line)
                        if (targets.winGoalEnabled) {
                            val goalPath = buildBuyInDerivedPath(extendToRightEdge = true) {
                                targets.winGoalDollars(it)
                            }
                            drawPath(
                                path = goalPath,
                                color = winGoalColor,
                                style = Stroke(width = targetStrokePx, pathEffect = dashEffect, cap = StrokeCap.Round)
                            )
                        }

                        // Series 2: Total Buy-In (Neutral Step Line)
                        val buyInPath = buildBuyInDerivedPath(extendToRightEdge = false) { it }
                        if (points.size == 1) {
                            val p0x = mapX(points[0].handNumber.toFloat())
                            val p0y = mapY(points[0].totalBuyInDollars)
                            drawCircle(color = buyInColor, radius = (2.5f * scale).dp.toPx(), center = Offset(p0x, p0y))
                        } else {
                            drawPath(
                                path = buyInPath,
                                color = buyInColor.copy(alpha = 0.85f),
                                style = Stroke(width = stepStrokePx, join = StrokeJoin.Miter)
                            )
                        }

                        // Series 1: Current Balance (Solid Primary Line)
                        if (points.size > 1) {
                            val balPath = Path()
                            points.forEachIndexed { idx, pt ->
                                val px = mapX(pt.handNumber.toFloat())
                                val preBal = pt.balanceBeforeInsert
                                if (idx == 0) {
                                    balPath.moveTo(px, mapY(pt.balanceDollars))
                                } else if (preBal != null) {
                                    balPath.lineTo(px, mapY(preBal))
                                    balPath.lineTo(px, mapY(pt.balanceDollars))
                                } else {
                                    balPath.lineTo(px, mapY(pt.balanceDollars))
                                }
                            }
                            drawPath(
                                path = balPath,
                                color = balanceColor,
                                style = Stroke(
                                    width = balanceStrokePx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Current Balance active point dot + Suboptimal Hold Red Dots
                        val lastPt = points.last()
                        val lastX = mapX(lastPt.handNumber.toFloat())
                        val lastY = mapY(lastPt.balanceDollars)
                        drawCircle(
                            color = palette.panelColor,
                            radius = (4.0f * scale).dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = balanceColor,
                            radius = (2.8f * scale).dp.toPx(),
                            center = Offset(lastX, lastY)
                        )

                        val haloR = (4.4f * scale).dp.toPx()
                        val dotR = (3.1f * scale).dp.toPx()
                        points.forEach { pt ->
                            if (pt.isSuboptimalHold) {
                                val px = mapX(pt.handNumber.toFloat())
                                val py = mapY(pt.balanceBeforeInsert ?: pt.balanceDollars)
                                drawCircle(color = palette.panelColor, radius = haloR, center = Offset(px, py))
                                drawCircle(color = suboptimalDotColor, radius = dotR, center = Offset(px, py))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    label: String,
    color: Color,
    isDashed: Boolean,
    isDot: Boolean,
    scale: Float,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((2.5f * scale).dp)
    ) {
        if (isDot) {
            Box(
                modifier = Modifier
                    .size((6f * scale).dp)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            Canvas(
                modifier = Modifier
                    .width((10f * scale).dp)
                    .height((4f * scale).dp)
            ) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = (1.8f * scale).dp.toPx(),
                    pathEffect = if (isDashed) {
                        PathEffect.dashPathEffect(floatArrayOf(3f * scale, 2f * scale), 0f)
                    } else null
                )
            }
        }
        Text(
            text = label,
            color = textColor,
            style = TextStyle(
                fontSize = (8.5f * scale).sp,
                fontWeight = FontWeight.SemiBold,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
