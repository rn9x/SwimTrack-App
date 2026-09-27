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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppSettings
import com.example.domain.model.ChartRangeFilter
import com.example.domain.model.PoolLength
import com.example.domain.model.SessionType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.domain.usecase.SwimUseCases
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SwimProgressionChart
import com.example.ui.components.SwimRecordCard
import com.example.ui.theme.ImprovementGreenContainerDark
import com.example.ui.theme.ImprovementGreenContainerLight
import com.example.ui.theme.ImprovementGreenDark
import com.example.ui.theme.ImprovementGreenLight
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    distance: SwimDistance,
    stroke: Stroke,
    allRecords: List<SwimRecord>,
    settings: AppSettings,
    onBack: () -> Unit,
    onEditRecordClick: (Long) -> Unit,
    onDeleteRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    var poolFilter by rememberSaveable { mutableStateOf<PoolLength?>(null) }
    var sessionFilter by rememberSaveable { mutableStateOf<SessionType?>(null) }
    var chartRange by rememberSaveable { mutableStateOf(ChartRangeFilter.ALL) }
    var recordToDelete by remember { mutableStateOf<SwimRecord?>(null) }

    val stats = remember(allRecords, distance, stroke, poolFilter, sessionFilter, settings.competitionOnlyPb) {
        SwimUseCases.computeEventStatistics(
            allRecords = allRecords,
            distance = distance,
            stroke = stroke,
            poolFilter = poolFilter,
            sessionFilter = sessionFilter,
            competitionOnlyPb = settings.competitionOnlyPb
        )
    }

    val chartRecords = remember(stats.chronologicalRecords, chartRange) {
        SwimUseCases.filterChartRecords(stats.chronologicalRecords, chartRange)
    }

    val reverseChronologicalRecords = remember(allRecords, distance, stroke, poolFilter, sessionFilter) {
        allRecords.filter {
            it.distance == distance &&
                it.stroke == stroke &&
                (poolFilter == null || it.poolLength == poolFilter) &&
                (sessionFilter == null || it.sessionType == sessionFilter)
        }.sortedByDescending { it.date }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("event_details_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back button
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("event_details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Back", "رجوع")
                    )
                }
                Column {
                    Text(
                        text = "${distance.localizedLabel(lang)} ${stroke.localizedName(lang)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Event Performance, Progression & Result Comparison",
                            "أداء السباق والتطور ومقارنة النتائج"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Pool & Session Filters (keeping 25m and 50m clearly separated when desired)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Pool Length", "طول المسبح"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = poolFilter == null,
                                    onClick = { poolFilter = null },
                                    label = { Text(tr("All", "الكل")) }
                                )
                                PoolLength.entries.forEach { pool ->
                                    FilterChip(
                                        selected = poolFilter == pool,
                                        onClick = {
                                            poolFilter = if (poolFilter == pool) null else pool
                                        },
                                        label = { Text(pool.localizedLabel(lang)) }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = tr("Session Type", "نوع الجلسة"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = sessionFilter == null,
                                    onClick = { sessionFilter = null },
                                    label = { Text(tr("All", "الكل")) }
                                )
                                SessionType.entries.forEach { session ->
                                    FilterChip(
                                        selected = sessionFilter == session,
                                        onClick = {
                                            sessionFilter = if (sessionFilter == session) null else session
                                        },
                                        label = { Text(session.localizedName(lang)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (stats.totalRecordsCount == 0) {
            item {
                EmptyStateCard(
                    title = tr(
                        "No results for ${distance.localizedLabel(lang)} ${stroke.localizedName(lang)}",
                        "لا توجد نتائج لسباق ${distance.localizedLabel(lang)} ${stroke.localizedName(lang)}"
                    ),
                    subtitle = tr(
                        "Record swims for this event or clear filters to see statistics and progression.",
                        "سجل سباحات لهذا السباق أو امسح عوامل التصفية لعرض الإحصائيات والتطور."
                    )
                )
            }
        } else {
            // Event Details Metrics Card (PB, Average, Latest, First, Best this month, Best this year, Total attempts)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EventMetricBox(
                                label = "PB",
                                value = stats.pbRecord?.let {
                                    SwimTimeUtils.formatSwimTime(it.timeMillis, settings.timeFormatPreference)
                                } ?: "—",
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                            EventMetricBox(
                                label = tr("Average", "المتوسط"),
                                value = stats.averageTimeMillis?.let {
                                    SwimTimeUtils.formatSwimTime(it, settings.timeFormatPreference)
                                } ?: "—",
                                highlight = false,
                                modifier = Modifier.weight(1f)
                            )
                            EventMetricBox(
                                label = tr("Attempts", "المحاولات"),
                                value = stats.totalRecordsCount.toString(),
                                highlight = false,
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EventMetricBox(
                                label = tr("Latest", "الأحدث"),
                                value = stats.latestRecord?.let {
                                    SwimTimeUtils.formatSwimTime(it.timeMillis, settings.timeFormatPreference)
                                } ?: "—",
                                highlight = false,
                                modifier = Modifier.weight(1f)
                            )
                            EventMetricBox(
                                label = tr("First", "الأول"),
                                value = stats.firstRecord?.let {
                                    SwimTimeUtils.formatSwimTime(it.timeMillis, settings.timeFormatPreference)
                                } ?: "—",
                                highlight = false,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EventMetricBox(
                                label = tr("Best This Month", "الأفضل هذا الشهر"),
                                value = stats.bestThisMonthMillis?.let {
                                    SwimTimeUtils.formatSwimTime(it, settings.timeFormatPreference)
                                } ?: "—",
                                highlight = false,
                                modifier = Modifier.weight(1f)
                            )
                            EventMetricBox(
                                label = tr("Best This Year", "الأفضل هذا العام"),
                                value = stats.bestThisYearMillis?.let {
                                    SwimTimeUtils.formatSwimTime(it, settings.timeFormatPreference)
                                } ?: "—",
                                highlight = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Progress Chart
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChartRangeFilter.entries.forEach { range ->
                            FilterChip(
                                selected = chartRange == range,
                                onClick = { chartRange = range },
                                label = { Text(range.localizedLabel(lang)) }
                            )
                        }
                    }
                    SwimProgressionChart(
                        records = chartRecords,
                        timeFormat = settings.timeFormatPreference
                    )
                }
            }

            // Section 12: Result Comparison Feature (Compare two results from the same event)
            if (stats.chronologicalRecords.size >= 2) {
                item {
                    ResultComparisonCard(
                        validChronologicalRecords = stats.chronologicalRecords,
                        pbRecord = stats.pbRecord,
                        settings = settings
                    )
                }
            }

            // Chronological History Header
            item {
                Text(
                    text = tr(
                        "History (${reverseChronologicalRecords.size})",
                        "السجل التاريخي (${reverseChronologicalRecords.size})"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(reverseChronologicalRecords, key = { it.id }) { record ->
                SwimRecordCard(
                    record = record,
                    timeFormat = settings.timeFormatPreference,
                    onCardClick = {},
                    onEditClick = { onEditRecordClick(record.id) },
                    onDeleteClick = { recordToDelete = record }
                )
            }
        }
    }

    recordToDelete?.let { record ->
        ConfirmDeleteDialog(
            title = tr("Delete Swim Record?", "حذف سجل السباحة؟"),
            message = tr(
                "Delete ${record.localizedEventTitle(lang)} (${SwimTimeUtils.formatSwimTime(record.timeMillis, settings.timeFormatPreference)}) from ${SwimTimeUtils.formatDateShort(record.date, lang)}?",
                "هل تريد حذف ${record.localizedEventTitle(lang)} (${SwimTimeUtils.formatSwimTime(record.timeMillis, settings.timeFormatPreference)}) بتاريخ ${SwimTimeUtils.formatDateShort(record.date, lang)}؟"
            ),
            onConfirm = { onDeleteRecordClick(record.id) },
            onDismiss = { recordToDelete = null }
        )
    }
}

@Composable
private fun EventMetricBox(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = SwimTimeTextStyles.StatValue,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ResultComparisonCard(
    validChronologicalRecords: List<SwimRecord>,
    pbRecord: SwimRecord?,
    settings: AppSettings
) {
    // Default to comparing First result (index 0) and Latest result (lastIndex)
    var baselineIndex by remember(validChronologicalRecords) {
        mutableIntStateOf(0)
    }
    var targetIndex by remember(validChronologicalRecords) {
        mutableIntStateOf(validChronologicalRecords.lastIndex)
    }

    val safeBaselineIdx = baselineIndex.coerceIn(0, validChronologicalRecords.lastIndex)
    val safeTargetIdx = targetIndex.coerceIn(0, validChronologicalRecords.lastIndex)

    val baselineRecord = validChronologicalRecords[safeBaselineIdx]
    val targetRecord = validChronologicalRecords[safeTargetIdx]

    val comparison = remember(baselineRecord, targetRecord) {
        SwimTimeUtils.calculateDifference(
            oldTimeMillis = baselineRecord.timeMillis,
            newTimeMillis = targetRecord.timeMillis
        )
    }

    var baselineExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    val lang = settings.language
    val isDark = isSystemInDarkTheme()
    val greenColor = if (isDark) ImprovementGreenDark else ImprovementGreenLight
    val greenBg = if (isDark) ImprovementGreenContainerDark else ImprovementGreenContainerLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comparison_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = tr("Compare Event Results", "مقارنة نتائج السباق"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Compare any two swims from ${baselineRecord.localizedEventTitle(lang)}",
                            "قارن بين أي نتيجتين في سباق ${baselineRecord.localizedEventTitle(lang)}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Comparison Presets: First vs Latest, PB vs Latest
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = safeBaselineIdx == 0 && safeTargetIdx == validChronologicalRecords.lastIndex,
                    onClick = {
                        baselineIndex = 0
                        targetIndex = validChronologicalRecords.lastIndex
                    },
                    label = { Text(tr("First vs Latest", "الأول مقابل الأحدث")) },
                    modifier = Modifier.testTag("compare_first_latest_chip")
                )

                val pbIdx = validChronologicalRecords.indexOfFirst { it.id == pbRecord?.id }
                if (pbIdx >= 0) {
                    FilterChip(
                        selected = safeBaselineIdx == validChronologicalRecords.lastIndex && safeTargetIdx == pbIdx,
                        onClick = {
                            baselineIndex = validChronologicalRecords.lastIndex
                            targetIndex = pbIdx
                        },
                        label = { Text(tr("Latest vs PB", "الأحدث مقابل الرقم القياسي")) },
                        modifier = Modifier.testTag("compare_latest_pb_chip")
                    )
                    FilterChip(
                        selected = safeBaselineIdx == 0 && safeTargetIdx == pbIdx,
                        onClick = {
                            baselineIndex = 0
                            targetIndex = pbIdx
                        },
                        label = { Text(tr("First vs PB", "الأول مقابل الرقم القياسي")) },
                        modifier = Modifier.testTag("compare_first_pb_chip")
                    )
                }
            }

            // Selectors for Result A (Baseline) and Result B (Target)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = baselineExpanded,
                    onExpandedChange = { baselineExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "${SwimTimeUtils.formatSwimTime(baselineRecord.timeMillis, settings.timeFormatPreference)} (${SwimTimeUtils.formatDateCompact(baselineRecord.date, lang)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Baseline (Old)", "الأساس (القديم)")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = baselineExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = baselineExpanded,
                        onDismissRequest = { baselineExpanded = false }
                    ) {
                        validChronologicalRecords.forEachIndexed { idx, rec ->
                            DropdownMenuItem(
                                text = {
                                    Text("${SwimTimeUtils.formatSwimTime(rec.timeMillis, settings.timeFormatPreference)} • ${SwimTimeUtils.formatDateShort(rec.date, lang)} (${rec.poolLength.localizedLabel(lang)})")
                                },
                                onClick = {
                                    baselineIndex = idx
                                    baselineExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "${SwimTimeUtils.formatSwimTime(targetRecord.timeMillis, settings.timeFormatPreference)} (${SwimTimeUtils.formatDateCompact(targetRecord.date, lang)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Compared (New)", "المقارن (الجديد)")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false }
                    ) {
                        validChronologicalRecords.forEachIndexed { idx, rec ->
                            DropdownMenuItem(
                                text = {
                                    Text("${SwimTimeUtils.formatSwimTime(rec.timeMillis, settings.timeFormatPreference)} • ${SwimTimeUtils.formatDateShort(rec.date, lang)} (${rec.poolLength.localizedLabel(lang)})")
                                },
                                onClick = {
                                    targetIndex = idx
                                    targetExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Pool Length Separation Warning if user selects 25m vs 50m
            if (baselineRecord.poolLength != targetRecord.poolLength) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr(
                                "Note: Comparing ${baselineRecord.poolLength.localizedLabel(lang)} pool vs ${targetRecord.poolLength.localizedLabel(lang)} pool. Short course and long course times are not directly equivalent.",
                                "ملاحظة: تتم المقارنة بين مسبح ${baselineRecord.poolLength.localizedLabel(lang)} ومسبح ${targetRecord.poolLength.localizedLabel(lang)}. أزمنة المجرى القصير والطويل غير متكافئة مباشرة."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Comparison Output Box
            Surface(
                color = if (comparison.isImprovement) greenBg else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (comparison.isImprovement) greenColor else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(
                            "${SwimTimeUtils.formatSwimTime(targetRecord.timeMillis, settings.timeFormatPreference)} vs ${SwimTimeUtils.formatSwimTime(baselineRecord.timeMillis, settings.timeFormatPreference)}",
                            "${SwimTimeUtils.formatSwimTime(targetRecord.timeMillis, settings.timeFormatPreference)} مقابل ${SwimTimeUtils.formatSwimTime(baselineRecord.timeMillis, settings.timeFormatPreference)}"
                        ),
                        style = SwimTimeTextStyles.ListPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("Difference:", "الفرق:"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val statusLabel = when {
                            comparison.isImprovement -> tr("Faster", "أسرع")
                            comparison.differenceMillis < 0 -> tr("Slower", "أبطأ")
                            else -> tr("Identical", "متطابق")
                        }
                        Text(
                            text = "${SwimTimeUtils.formatAbsoluteDifference(comparison.differenceMillis, lang)} ($statusLabel)",
                            style = SwimTimeTextStyles.StatValue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("comparison_difference_text")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("Percentage improvement:", "نسبة التحسن:"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = comparison.formattedPercentage,
                            style = SwimTimeTextStyles.StatValue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("comparison_percentage_text")
                        )
                    }
                }
            }
        }
    }
}
