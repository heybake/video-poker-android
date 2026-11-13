package com.videopoker.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Speaker Icon Canvas
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onTogglePopup() },
            contentAlignment = Alignment.Center
        ) {
            val iconColor = palette.soundIconColor
            Canvas(modifier = Modifier.size(18.dp)) {
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
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
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
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Volume Popup Slider
        if (isPopupOpen) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.panelColor)
                    .border(1.dp, palette.panelBorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VOL",
                        color = palette.goldColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.goldColor,
                            activeTrackColor = palette.goldColor,
                            inactiveTrackColor = Color(0x44FFFFFF)
                        )
                    )
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = palette.textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
