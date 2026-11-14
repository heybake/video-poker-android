package com.videopoker.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun SoundButton(
    volume: Float,
    isPopupOpen: Boolean,
    palette: ThemePalette,
    onTogglePopup: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = LocalResponsiveMetrics.current.uiScale

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Speaker Icon Canvas (Click to toggle volume slider popup)
        Box(
            modifier = Modifier
                .size((24f * scale).dp)
                .clickable { onTogglePopup() },
            contentAlignment = Alignment.Center
        ) {
            val iconColor = palette.soundIconColor
            Canvas(modifier = Modifier.size((18f * scale).dp)) {
                val w = size.width
                val h = size.height

                // Speaker horn path
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.35f)
                    lineTo(w * 0.35f, h * 0.35f)
                    lineTo(w * 0.65f, h * 0.15f)
                    lineTo(w * 0.65f, h * 0.85f)
                    lineTo(w * 0.35f, h * 0.65f)
                    lineTo(w * 0.15f, h * 0.65f)
                    close()
                }
                drawPath(path = path, color = iconColor)

                // Sound waves if volume > 0
                if (volume > 0.05f) {
                    drawArc(
                        color = iconColor,
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * 0.45f, h * 0.25f),
                        size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f),
                        style = Stroke(width = (2f * scale).dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                if (volume > 0.5f) {
                    drawArc(
                        color = iconColor,
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * 0.55f, h * 0.1f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.8f),
                        style = Stroke(width = (2f * scale).dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Floating Volume Popup Slider (floats ABOVE the status bar; dismisses on outside tap or X)
        if (isPopupOpen) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, (-110 * scale).toInt()),
                onDismissRequest = onTogglePopup
            ) {
                Box(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.overlayBgColor)
                        .border(1.5.dp, palette.goldColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = (10f * scale).dp, vertical = (6f * scale).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)
                    ) {
                        Text(
                            text = "VOL",
                            color = palette.goldColor,
                            fontSize = (11f * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.width((120f * scale).dp),
                            colors = SliderDefaults.colors(
                                thumbColor = palette.goldColor,
                                activeTrackColor = palette.goldColor,
                                inactiveTrackColor = Color(0x44FFFFFF)
                            )
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            color = Color.White,
                            fontSize = (11f * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Close button '✕' to return to just the volume icon
                        Box(
                            modifier = Modifier
                                .size((20f * scale).dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onTogglePopup() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontSize = (11f * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
