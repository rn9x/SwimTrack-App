package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.domain.model.TimeFormatPreference
import com.example.ui.theme.ImprovementGreenDark
import com.example.ui.theme.ImprovementGreenLight
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PbGoldDark
import com.example.ui.theme.PbGoldLight
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr
import kotlin.math.abs

/**
 * Performance progression chart where:
 * - X-axis = Chronological Date
 * - Y-axis = Swimming Time (inverted so lower/faster times rise higher on the performance curve,
 *   clearly labeled with exact time ticks so it is never misleading).
 */
@Composable
fun SwimProgressionChart(
    records: List<SwimRecord>,
    timeFormat: TimeFormatPreference,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val validRecords = remember(records) {
        records.filter { it.isValidForPb }.sortedBy { it.date }
    }

    if (validRecords.isEmpty()) {
        EmptyStateCard(
            title = tr("No chart data available", "لا توجد بيانات للرسم البياني"),
            subtitle = tr(
                "Add valid finished results for this event to visualize your progression curve.",
                "أضف نتائج مكتملة لهذا السباق لعرض منحنى تطور أدائك."
            ),
            modifier = modifier
        )
        return
    }

    var selectedIndex by remember(validRecords) {
        mutableIntStateOf(validRecords.lastIndex)
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, validRecords.lastIndex)
    val selectedRecord = validRecords[safeSelectedIndex]

    val minTime = validRecords.minOf { it.timeMillis }
    val maxTime = validRecords.maxOf { it.timeMillis }
    val midTime = (minTime + maxTime) / 2L

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val isDark = isSystemInDarkTheme()
    val pbColor = if (isDark) PbGoldDark else PbGoldLight
    val greenColor = if (isDark) ImprovementGreenDark else ImprovementGreenLight

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("progression_chart_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Performance Progression", "منحنى تطور الأداء"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "↑ Higher on chart = Faster swim time (${validRecords.size} swims)",
                            "↑ الأعلى في الرسم = زمن سباحة أسرع (${validRecords.size} سباقات)"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = greenColor
                    )
                }

                // Selected point callout pill
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SwimTimeUtils.formatSwimTime(selectedRecord.timeMillis, timeFormat),
                                style = SwimTimeTextStyles.StatValue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${SwimTimeUtils.formatDateShort(selectedRecord.date, lang)} • ${selectedRecord.poolLength.localizedLabel(lang)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (selectedRecord.isPersonalBest) {
                            PbBadge()
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-axis time labels (Top = Fastest/Lowest time, Bottom = Slowest/Highest time)
                Column(
                    modifier = Modifier
                        .height(190.dp)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = SwimTimeUtils.formatSwimTime(minTime, timeFormat),
                        style = SwimTimeTextStyles.SplitBadge,
                        color = pbColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (maxTime != minTime) {
                        Text(
                            text = SwimTimeUtils.formatSwimTime(midTime, timeFormat),
                            style = SwimTimeTextStyles.SplitBadge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SwimTimeUtils.formatSwimTime(maxTime, timeFormat),
                            style = SwimTimeTextStyles.SplitBadge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Canvas Plot
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(190.dp)
                        .pointerInput(validRecords) {
                            detectTapGestures { tapOffset ->
                                if (validRecords.size == 1) {
                                    selectedIndex = 0
                                } else {
                                    val padH = 16.dp.toPx()
                                    val usableWidth = (size.width - padH * 2).coerceAtLeast(1f)
                                    val stepX = usableWidth / (validRecords.size - 1)
                                    val nearest = ((tapOffset.x - padH) / stepX)
                                        .toInt()
                                        .coerceIn(0, validRecords.lastIndex)
                                    selectedIndex = nearest
                                }
                            }
                        }
                ) {
                    val padH = 16.dp.toPx()
                    val padV = 16.dp.toPx()
                    val plotW = (size.width - padH * 2).coerceAtLeast(1f)
                    val plotH = (size.height - padV * 2).coerceAtLeast(1f)

                    // Horizontal guide lines (top, middle, bottom)
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = padV + plotH * frac
                        drawLine(
                            color = gridColor,
                            start = Offset(padH, y),
                            end = Offset(size.width - padH, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                    val points = validRecords.mapIndexed { index, record ->
                        val x = if (validRecords.size == 1) {
                            size.width / 2f
                        } else {
                            padH + (index.toFloat() / (validRecords.size - 1)) * plotW
                        }
                        // Lower time -> smaller normalized (0f = top of chart = fastest!)
                        val normalizedY = if (maxTime == minTime) {
                            0.5f
                        } else {
                            (record.timeMillis - minTime).toFloat() / timeRange.toFloat()
                        }
                        val y = padV + normalizedY * plotH
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, size.height - padV)
                            lineTo(points.first().x, size.height - padV)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = padV,
                                endY = size.height - padV
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw points
                    points.forEachIndexed { index, offset ->
                        val rec = validRecords[index]
                        val isSelected = index == safeSelectedIndex
                        val isPb = rec.timeMillis == minTime

                        if (isSelected) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.35f),
                                start = Offset(offset.x, padV),
                                end = Offset(offset.x, size.height - padV),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.22f),
                                radius = 11.dp.toPx(),
                                center = offset
                            )
                        }

                        drawCircle(
                            color = if (isPb) pbColor else primaryColor,
                            radius = if (isSelected || isPb) 6.dp.toPx() else 4.5.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }

            // X-axis Start & End Dates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SwimTimeUtils.formatDateCompact(validRecords.first().date, lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (validRecords.size > 2) {
                    Text(
                        text = SwimTimeUtils.formatDateCompact(validRecords[validRecords.size / 2].date, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (validRecords.size > 1) {
                    Text(
                        text = SwimTimeUtils.formatDateCompact(validRecords.last().date, lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
