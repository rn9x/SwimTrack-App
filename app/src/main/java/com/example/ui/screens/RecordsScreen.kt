package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppSettings
import com.example.domain.model.PoolLength
import com.example.domain.model.SessionType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.ui.RecordsFilterState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SwimRecordCard
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(
    records: List<SwimRecord>,
    totalUnfilteredCount: Int,
    filterState: RecordsFilterState,
    settings: AppSettings,
    onStrokeFilterChange: (Stroke?) -> Unit,
    onDistanceFilterChange: (SwimDistance?) -> Unit,
    onSessionFilterChange: (SessionType?) -> Unit,
    onPoolFilterChange: (PoolLength?) -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onAddRecordClick: () -> Unit,
    onOpenEventDetails: (SwimDistance, Stroke) -> Unit,
    onEditRecordClick: (Long) -> Unit,
    onDeleteRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    var showFiltersPanel by rememberSaveable { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<SwimRecord?>(null) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("records_screen")
    ) {
        // Top Header + Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tr("Swim History", "سجل السباحة"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "${records.size} of $totalUnfilteredCount records • Sorted newest first",
                            "${records.size} من أصل $totalUnfilteredCount سجل • مرتبة من الأحدث"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (filterState.hasActiveFilters) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.testTag("clear_filters_button")
                        ) {
                            Text(tr("Reset", "إعادة ضبط"))
                        }
                    }
                    FilterChip(
                        selected = showFiltersPanel || filterState.hasActiveFilters,
                        onClick = { showFiltersPanel = !showFiltersPanel },
                        label = { Text(tr("Filters", "تصفية")) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = tr("Toggle Filters", "إظهار التصفية"),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("toggle_filters_button")
                    )
                }
            }

            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(tr("Search event, competition, or notes...", "ابحث عن سباق، بطولة، أو ملاحظات...")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (filterState.searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = tr("Clear search", "مسح البحث"))
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_records_input")
            )

            AnimatedVisibility(visible = showFiltersPanel) {
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
                        // Stroke Filter
                        Text(
                            text = tr("Stroke", "نوع السباحة"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterState.stroke == null,
                                onClick = { onStrokeFilterChange(null) },
                                label = { Text(tr("All", "الكل")) }
                            )
                            Stroke.entries.forEach { stroke ->
                                FilterChip(
                                    selected = filterState.stroke == stroke,
                                    onClick = {
                                        onStrokeFilterChange(
                                            if (filterState.stroke == stroke) null else stroke
                                        )
                                    },
                                    label = { Text(stroke.localizedShortName(lang)) },
                                    modifier = Modifier.testTag("filter_stroke_${stroke.name}")
                                )
                            }
                        }

                        // Distance Filter
                        Text(
                            text = tr("Distance", "المسافة"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterState.distance == null,
                                onClick = { onDistanceFilterChange(null) },
                                label = { Text(tr("All", "الكل")) }
                            )
                            SwimDistance.entries.forEach { dist ->
                                FilterChip(
                                    selected = filterState.distance == dist,
                                    onClick = {
                                        onDistanceFilterChange(
                                            if (filterState.distance == dist) null else dist
                                        )
                                    },
                                    label = { Text(dist.localizedLabel(lang)) },
                                    modifier = Modifier.testTag("filter_distance_${dist.label}")
                                )
                            }
                        }

                        // Session Type & Pool Length Filters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = tr("Session Type", "نوع الجلسة"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
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
                                            modifier = Modifier.testTag("filter_session_${session.name}")
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(0.9f)) {
                                Text(
                                    text = tr("Pool Length", "طول المسبح"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
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
                                            modifier = Modifier.testTag("filter_pool_${pool.label}")
                                        )
                                    }
                                }
                            }
                        }

                        // Date Range Presets
                        Text(
                            text = tr("Date Range", "الفترة الزمنية"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val now = System.currentTimeMillis()
                        val dayMs = 24 * 60 * 60 * 1000L
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = filterState.startDateMillis == null,
                                onClick = { onDateRangeChange(null, null) },
                                label = { Text(tr("All Time", "كل الأوقات")) }
                            )
                            FilterChip(
                                selected = filterState.startDateMillis != null &&
                                    (now - (filterState.startDateMillis ?: 0L)) in (6 * dayMs)..(8 * dayMs),
                                onClick = { onDateRangeChange(now - 7 * dayMs, null) },
                                label = { Text(tr("Last 7 Days", "آخر 7 أيام")) }
                            )
                            FilterChip(
                                selected = filterState.startDateMillis != null &&
                                    (now - (filterState.startDateMillis ?: 0L)) in (29 * dayMs)..(31 * dayMs),
                                onClick = { onDateRangeChange(now - 30 * dayMs, null) },
                                label = { Text(tr("Last 30 Days", "آخر 30 يوماً")) }
                            )
                            FilterChip(
                                selected = filterState.startDateMillis != null &&
                                    (now - (filterState.startDateMillis ?: 0L)) > 60 * dayMs,
                                onClick = { onDateRangeChange(now - 365 * dayMs, null) },
                                label = { Text(tr("Last 12 Months", "آخر 12 شهراً")) }
                            )
                        }
                    }
                }
            }
        }

        // Records List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("records_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (totalUnfilteredCount == 0) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Pool,
                        title = tr("No swimming records yet", "لا توجد سجلات سباحة بعد"),
                        subtitle = tr(
                            "Add your first result to start tracking your progress.",
                            "أضف أول نتيجة لك لبدء تتبع تطورك وأرقامك القياسية."
                        ),
                        actionLabel = tr("Add Record", "إضافة سجل"),
                        onActionClick = onAddRecordClick
                    )
                }
            } else if (records.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.FilterList,
                        title = tr("No matching records", "لا توجد سجلات مطابقة"),
                        subtitle = tr(
                            "Try adjusting your stroke, distance, pool length, or date filters.",
                            "جرّب تعديل عوامل التصفية حسب نوع السباحة أو المسافة أو المسبح."
                        ),
                        actionLabel = tr("Clear Filters", "مسح التصفية"),
                        onActionClick = onClearFilters
                    )
                }
            } else {
                items(records, key = { it.id }) { record ->
                    SwimRecordCard(
                        record = record,
                        timeFormat = settings.timeFormatPreference,
                        onCardClick = { onOpenEventDetails(record.distance, record.stroke) },
                        onEditClick = { onEditRecordClick(record.id) },
                        onDeleteClick = { recordToDelete = record }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    recordToDelete?.let { record ->
        val formattedTime = SwimTimeUtils.formatSwimTime(record.timeMillis, settings.timeFormatPreference)
        ConfirmDeleteDialog(
            title = tr("Delete Swim Record?", "حذف سجل السباحة؟"),
            message = tr(
                "Are you sure you want to delete this ${record.eventTitle} result ($formattedTime)?",
                "هل أنت متأكد من حذف نتيجة ${record.localizedEventTitle(lang)} ($formattedTime)؟"
            ),
            onConfirm = { onDeleteRecordClick(record.id) },
            onDismiss = { recordToDelete = null }
        )
    }
}
