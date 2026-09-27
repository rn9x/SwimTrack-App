package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppSettings
import com.example.domain.model.ChartRangeFilter
import com.example.domain.model.CompetitionWithResults
import com.example.domain.model.EventStatistics
import com.example.domain.model.PersonalBestSummary
import com.example.domain.model.PoolLength
import com.example.domain.model.SessionType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.domain.usecase.SwimUseCases
import com.example.ui.StatisticsFilterState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SwimProgressionChart
import com.example.ui.theme.ImprovementGreenContainerDark
import com.example.ui.theme.ImprovementGreenContainerLight
import com.example.ui.theme.ImprovementGreenDark
import com.example.ui.theme.ImprovementGreenLight
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    allRecords: List<SwimRecord>,
    personalBests: List<PersonalBestSummary>,
    competitions: List<CompetitionWithResults>,
    filterState: StatisticsFilterState,
    eventStatistics: EventStatistics,
    settings: AppSettings,
    onSelectEvent: (SwimDistance, Stroke) -> Unit,
    onSessionFilterChange: (SessionType?) -> Unit,
    onPoolFilterChange: (PoolLength?) -> Unit,
    onChartRangeChange: (ChartRangeFilter) -> Unit,
    onOpenEventDetails: (SwimDistance, Stroke) -> Unit,
    onAddRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    val totalRecords = allRecords.size
    val totalCompetitions = competitions.size
    val totalTrainingSessions = remember(allRecords) {
        allRecords.count { it.sessionType == SessionType.TRAINING }
    }
    val totalPbs = personalBests.size

    val chartRecords = remember(eventStatistics.chronologicalRecords, filterState.chartRange) {
        SwimUseCases.filterChartRecords(
            chronologicalRecords = eventStatistics.chronologicalRecords,
            rangeFilter = filterState.chartRange
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("statistics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = tr("Statistics & Analytics", "الإحصائيات والتحليلات"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tr(
                    "Analyze personal bests, averages, progression curves, and improvements.",
                    "حلل الأرقام القياسية والمتوسطات ومنحنيات التطور والتحسن."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Global Overview Totals (Total records, Total competitions, Total training sessions, Total PBs)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlobalStatMiniCard(
                    label = tr("Records", "السجلات"),
                    value = totalRecords.toString(),
                    modifier = Modifier.weight(1f)
                )
                GlobalStatMiniCard(
                    label = tr("Competitions", "البطولات"),
                    value = totalCompetitions.toString(),
                    modifier = Modifier.weight(1f)
                )
                GlobalStatMiniCard(
                    label = tr("Training", "التدريبات"),
                    value = totalTrainingSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
                GlobalStatMiniCard(
                    label = tr("Total PBs", "إجمالي PB"),
                    value = totalPbs.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (allRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Analytics,
                    title = tr("No statistics available yet", "لا تتوفر إحصائيات بعد"),
                    subtitle = tr(
                        "Record training or competition swims to unlock detailed performance analytics.",
                        "سجل سباحات التدريب أو البطولات لفتح تحليلات الأداء التفصيلية."
                    ),
                    actionLabel = tr("Add Record", "إضافة سجل"),
                    onActionClick = onAddRecordClick
                )
            }
        } else {
            // 2. Event & Category Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr("Select Event", "اختر السباق"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Distance Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SwimDistance.entries.forEach { dist ->
                                FilterChip(
                                    selected = filterState.distance == dist,
                                    onClick = { onSelectEvent(dist, filterState.stroke) },
                                    label = { Text(dist.localizedLabel(lang)) },
                                    modifier = Modifier.testTag("stats_distance_${dist.label}")
                                )
                            }
                        }

                        // Stroke Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Stroke.entries.forEach { stroke ->
                                FilterChip(
                                    selected = filterState.stroke == stroke,
                                    onClick = { onSelectEvent(filterState.distance, stroke) },
                                    label = { Text(stroke.localizedName(lang)) },
                                    modifier = Modifier.testTag("stats_stroke_${stroke.name}")
                                )
                            }
                        }

                        // Session Type & Pool Length Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = tr("Category Filter", "تصفية الفئة"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = filterState.sessionType == null,
                                        onClick = { onSessionFilterChange(null) },
                                        label = { Text(tr("All", "الكل")) }
                                    )
                                    SessionType.entries.forEach { session ->
                                        FilterChip(
                                            selected = filterState.sessionType == session,
                                            onClick = {
                                                onSessionFilterChange(
                                                    if (filterState.sessionType == session) null else session
                                                )
                                            },
                                            label = { Text(session.localizedName(lang)) },
                                            modifier = Modifier.testTag("stats_session_${session.name}")
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(0.9f)) {
                                Text(
                                    text = tr("Pool Length", "طول المسبح"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = filterState.poolLength == null,
                                        onClick = { onPoolFilterChange(null) },
                                        label = { Text(tr("All", "الكل")) }
                                    )
                                    PoolLength.entries.forEach { pool ->
                                        FilterChip(
                                            selected = filterState.poolLength == pool,
                                            onClick = {
                                                onPoolFilterChange(
                                                    if (filterState.poolLength == pool) null else pool
                                                )
                                            },
                                            label = { Text(pool.localizedLabel(lang)) },
                                            modifier = Modifier.testTag("stats_pool_${pool.label}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Event Statistics Card
            item {
                EventStatsSummaryCard(
                    stats = eventStatistics,
                    settings = settings,
                    onOpenEventDetails = {
                        onOpenEventDetails(filterState.distance, filterState.stroke)
                    }
                )
            }

            // 4. Progress Chart with Range Filters (All, Last 10, Last 30, This Year)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(
                                "${eventStatistics.eventKey.localizedName(lang)} Progress",
                                "تطور ${eventStatistics.eventKey.localizedName(lang)}"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChartRangeFilter.entries.forEach { range ->
                            FilterChip(
                                selected = filterState.chartRange == range,
                                onClick = { onChartRangeChange(range) },
                                label = { Text(range.localizedLabel(lang)) },
                                modifier = Modifier.testTag("chart_range_${range.name}")
                            )
                        }
                    }

                    SwimProgressionChart(
                        records = chartRecords,
                        timeFormat = settings.timeFormatPreference
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalStatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = SwimTimeTextStyles.ListPrimary,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EventStatsSummaryCard(
    stats: EventStatistics,
    settings: AppSettings,
    onOpenEventDetails: () -> Unit
) {
    val lang = settings.language
    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) ImprovementGreenDark else ImprovementGreenLight
    val greenBg = if (isDark) ImprovementGreenContainerDark else ImprovementGreenContainerLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_statistics_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tr(
                            "${stats.eventKey.localizedName(lang)} Statistics",
                            "إحصائيات ${stats.eventKey.localizedName(lang)}"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Records: ${stats.totalRecordsCount} (${stats.validRecordsCount} valid finished)",
                            "السجلات: ${stats.totalRecordsCount} (${stats.validRecordsCount} مكتملة صحيحة)"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (stats.validRecordsCount == 0) {
                Text(
                    text = tr(
                        "No valid finished swims recorded for ${stats.eventKey.localizedName(lang)} with the current filters.",
                        "لا توجد سباحات مكتملة مسجلة لسباق ${stats.eventKey.localizedName(lang)} مع عوامل التصفية الحالية."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Row 1: PB & Average
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatHighlightTile(
                        label = tr("PB (Best Time)", "أفضل زمن (PB)"),
                        value = stats.pbRecord?.let {
                            SwimTimeUtils.formatSwimTime(it.timeMillis, settings.timeFormatPreference)
                        } ?: "—",
                        subLabel = stats.pbRecord?.let {
                            "${it.poolLength.localizedLabel(lang)} • ${SwimTimeUtils.formatDateCompact(it.date, lang)}"
                        } ?: "",
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatHighlightTile(
                        label = tr("Average", "المتوسط"),
                        value = stats.averageTimeMillis?.let {
                            SwimTimeUtils.formatSwimTime(it, settings.timeFormatPreference)
                        } ?: "—",
                        subLabel = tr("Across ${stats.validRecordsCount} swims", "خلال ${stats.validRecordsCount} سباحات"),
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: First Recorded, Latest, Worst
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSmallTile(
                        label = tr("First recorded", "أول زمن مسجل"),
                        value = stats.firstRecord?.let {
                            SwimTimeUtils.formatSwimTime(it.timeMillis, settings.timeFormatPreference)
                        } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatSmallTile(
                        label = tr("Latest", "الأحدث"),
                        value = stats.latestRecord?.let {
                            SwimTimeUtils.formatSwimTime(it.timeMillis, settings.timeFormatPreference)
                        } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatSmallTile(
                        label = tr("Worst time", "أبطأ زمن"),
                        value = stats.worstTimeMillis?.let {
                            SwimTimeUtils.formatSwimTime(it, settings.timeFormatPreference)
                        } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Improvement Banner from first recorded result
                stats.improvementFromFirst?.let { improvement ->
                    Surface(
                        color = if (improvement.isImprovement) greenBg else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (improvement.isImprovement) greenColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr("Improvement from First:", "التحسن منذ البداية:"),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Text(
                                text = "${SwimTimeUtils.formatAbsoluteDifference(improvement.differenceMillis, lang)} (${improvement.formattedPercentage})",
                                style = SwimTimeTextStyles.StatValue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("stats_improvement_value")
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenEventDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_event_details_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Event Details & Compare Results", "تفاصيل السباق ومقارنة النتائج"))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatHighlightTile(
    label: String,
    value: String,
    subLabel: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = SwimTimeTextStyles.CardLarge,
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (subLabel.isNotBlank()) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatSmallTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = SwimTimeTextStyles.StatValue,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
