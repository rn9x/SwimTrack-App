package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppSettings
import com.example.domain.model.Competition
import com.example.domain.model.CompetitionResult
import com.example.domain.model.CompetitionWithResults
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.StartType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimTimeUtils
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PbBadge
import com.example.ui.components.PoolBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.StatusDisqualifiedContainerDark
import com.example.ui.theme.StatusDisqualifiedContainerLight
import com.example.ui.theme.StatusDisqualifiedRed
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr

@Composable
fun CompetitionsScreen(
    competitions: List<CompetitionWithResults>,
    onOpenCompetitionDetails: (Long) -> Unit,
    onSaveCompetition: (Competition, (Long) -> Unit) -> Unit,
    onDeleteCompetition: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var showCreateCompetitionDialog by rememberSaveable { mutableStateOf(false) }
    var competitionToEdit by remember { mutableStateOf<Competition?>(null) }
    var competitionToDelete by remember { mutableStateOf<Competition?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("competitions_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tr("Competitions", "البطولات"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "Track meet entries, heats, lanes, seed times & places",
                        "تتبع المشاركات والتصفيات والحارات وأزمنة التسجيل والمراكز"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateCompetitionDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("create_competition_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("New Meet", "بطولة جديدة"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (competitions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.EmojiEvents,
                        title = tr("No competitions yet", "لا توجد بطولات بعد"),
                        subtitle = tr(
                            "Create a competition to log heats, lanes, seed times, and official results.",
                            "أنشئ بطولة لتسجيل التصفيات والحارات وأزمنة التسجيل والنتائج الرسمية."
                        ),
                        actionLabel = tr("Create Competition", "إنشاء بطولة"),
                        onActionClick = { showCreateCompetitionDialog = true }
                    )
                }
            } else {
                items(competitions, key = { it.competition.id }) { item ->
                    val comp = item.competition
                    val pbCount = item.results.count { it.isPersonalBest }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCompetitionDetails(comp.id) }
                            .testTag("competition_card_${comp.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comp.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = SwimTimeUtils.formatDateShort(comp.date, lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (comp.location.isNotBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = comp.location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { competitionToEdit = comp },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = tr("Edit competition", "تعديل البطولة"),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { competitionToDelete = comp },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_competition_${comp.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = tr("Delete competition", "حذف البطولة"),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (comp.notes.isNotBlank()) {
                                Text(
                                    text = comp.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            "${item.results.size} Events Entered",
                                            "${item.results.size} سباقات مسجلة"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (pbCount > 0) {
                                    PbBadge(text = "$pbCount PB")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCompetitionDialog || competitionToEdit != null) {
        CompetitionFormDialog(
            existingCompetition = competitionToEdit,
            onDismiss = {
                showCreateCompetitionDialog = false
                competitionToEdit = null
            },
            onSave = { comp ->
                val isNew = comp.id == 0L
                onSaveCompetition(comp) { savedId ->
                    showCreateCompetitionDialog = false
                    competitionToEdit = null
                    if (isNew) {
                        onOpenCompetitionDetails(savedId)
                    }
                }
            }
        )
    }

    competitionToDelete?.let { comp ->
        ConfirmDeleteDialog(
            title = tr("Delete Competition?", "حذف البطولة؟"),
            message = tr(
                "Delete \"${comp.name}\" and all its entered competition events?",
                "هل تريد حذف \"${comp.name}\" وجميع سباقاتها المسجلة؟"
            ),
            onConfirm = { onDeleteCompetition(comp.id) },
            onDismiss = { competitionToDelete = null }
        )
    }
}

@Composable
fun CompetitionDetailsScreen(
    competitionWithResults: CompetitionWithResults?,
    settings: AppSettings,
    onBack: () -> Unit,
    onSaveResult: (CompetitionResult, Long) -> Unit,
    onDeleteResult: (CompetitionResult) -> Unit,
    onOpenEventDetails: (SwimDistance, Stroke) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    var showAddEventDialog by rememberSaveable { mutableStateOf(false) }
    var resultToEdit by remember { mutableStateOf<CompetitionResult?>(null) }
    var resultToDelete by remember { mutableStateOf<CompetitionResult?>(null) }

    if (competitionWithResults == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "رجوع"))
            }
            EmptyStateCard(
                title = tr("Competition not found", "البطولة غير موجودة"),
                subtitle = tr("This competition may have been removed.", "ربما تم حذف هذه البطولة.")
            )
        }
        return
    }

    val comp = competitionWithResults.competition
    val results = competitionWithResults.results

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("competition_details_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("competition_details_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "رجوع"))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = comp.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SwimTimeUtils.formatDateShort(comp.date, lang) +
                                (if (comp.location.isNotBlank()) " • ${comp.location}" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showAddEventDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("add_competition_event_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Add Event", "إضافة سباق"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (results.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.EmojiEvents,
                    title = tr("No events entered yet", "لا توجد سباقات مسجلة بعد"),
                    subtitle = tr(
                        "Add your swimming events for ${comp.name} including heat, lane, seed time, official result, and place.",
                        "أضف سباقات السباحة الخاصة بك في ${comp.name} شاملة التصفية والحارة وزمن التسجيل والنتيجة الرسمية والمركز."
                    ),
                    actionLabel = tr("Add Competition Event", "إضافة سباق للبطولة"),
                    onActionClick = { showAddEventDialog = true }
                )
            }
        } else {
            items(results, key = { it.id }) { res ->
                CompetitionResultCard(
                    result = res,
                    settings = settings,
                    onClick = { onOpenEventDetails(res.distance, res.stroke) },
                    onEdit = { resultToEdit = res },
                    onDelete = { resultToDelete = res }
                )
            }
        }
    }

    if (showAddEventDialog || resultToEdit != null) {
        CompetitionResultFormDialog(
            competitionId = comp.id,
            existingResult = resultToEdit,
            defaultPoolLength = settings.defaultPoolLength,
            onDismiss = {
                showAddEventDialog = false
                resultToEdit = null
            },
            onSave = { newResult ->
                onSaveResult(newResult, comp.date)
                showAddEventDialog = false
                resultToEdit = null
            }
        )
    }

    resultToDelete?.let { res ->
        ConfirmDeleteDialog(
            title = tr("Delete Competition Event?", "حذف سباق البطولة؟"),
            message = tr(
                "Remove ${res.localizedEventTitle(lang)} from ${comp.name}?",
                "إزالة ${res.localizedEventTitle(lang)} من ${comp.name}؟"
            ),
            onConfirm = { onDeleteResult(res) },
            onDismiss = { resultToDelete = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompetitionResultCard(
    result: CompetitionResult,
    settings: AppSettings,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = settings.language
    val isDark = isSystemInDarkTheme()
    val isDisqualifiedOrNonFinish = result.status != ResultStatus.FINISHED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("competition_result_card_${result.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisqualifiedOrNonFinish) {
                if (isDark) StatusDisqualifiedContainerDark.copy(alpha = 0.35f) else StatusDisqualifiedContainerLight.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDisqualifiedOrNonFinish) {
                StatusDisqualifiedRed.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.localizedEventTitle(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PoolBadge(poolLength = result.poolLength)
                    if (result.isPersonalBest && result.status == ResultStatus.FINISHED) {
                        PbBadge()
                    }
                    if (result.status != ResultStatus.FINISHED) {
                        StatusBadge(status = result.status)
                    }
                }

                // Official Result Time or DQ/DNS/NS Status
                Text(
                    text = if (result.status == ResultStatus.FINISHED && result.resultTimeMillis != null) {
                        SwimTimeUtils.formatSwimTime(result.resultTimeMillis, settings.timeFormatPreference)
                    } else {
                        result.status.localizedBadge(lang)
                    },
                    style = SwimTimeTextStyles.CardLarge,
                    color = if (result.status == ResultStatus.FINISHED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        StatusDisqualifiedRed
                    }
                )
            }

            // Heat, Lane, Seed, Result, Place, Status chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (result.heat != null) {
                    MeetMetaPill(label = tr("Heat: ${result.heat}", "التصفية: ${result.heat}"))
                }
                if (result.lane != null) {
                    MeetMetaPill(label = tr("Lane: ${result.lane}", "الحارة: ${result.lane}"))
                }
                if (result.seedTimeMillis != null && result.seedTimeMillis > 0L) {
                    MeetMetaPill(
                        label = tr(
                            "Seed: ${SwimTimeUtils.formatSwimTime(result.seedTimeMillis, settings.timeFormatPreference)}",
                            "التسجيل: ${SwimTimeUtils.formatSwimTime(result.seedTimeMillis, settings.timeFormatPreference)}"
                        )
                    )
                }
                if (result.resultTimeMillis != null && result.resultTimeMillis > 0L) {
                    MeetMetaPill(
                        label = tr(
                            "Result: ${SwimTimeUtils.formatSwimTime(result.resultTimeMillis, settings.timeFormatPreference)}",
                            "النتيجة: ${SwimTimeUtils.formatSwimTime(result.resultTimeMillis, settings.timeFormatPreference)}"
                        )
                    )
                }
                if (result.place != null) {
                    MeetMetaPill(label = tr("Place: ${result.place}", "المركز: ${result.place}"))
                }
                MeetMetaPill(
                    label = tr(
                        "Status: ${result.status.localizedName(lang)}",
                        "الحالة: ${result.status.localizedName(lang)}"
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (result.status != ResultStatus.FINISHED) {
                        tr(
                            "Excluded from PB & average calculations (${result.status.localizedBadge(lang)})",
                            "مستبعد من حسابات الرقم القياسي والمتوسط (${result.status.localizedBadge(lang)})"
                        )
                    } else {
                        result.notes.ifBlank {
                            tr(
                                "${result.startType.localizedName(lang)} • Official Competition Result",
                                "${result.startType.localizedName(lang)} • نتيجة بطولة رسمية"
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.status != ResultStatus.FINISHED) {
                        StatusDisqualifiedRed
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = tr("Edit event", "تعديل السباق"),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = tr("Delete event", "حذف السباق"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetMetaPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionFormDialog(
    existingCompetition: Competition?,
    onDismiss: () -> Unit,
    onSave: (Competition) -> Unit
) {
    val lang = LocalAppLanguage.current
    var name by rememberSaveable { mutableStateOf(existingCompetition?.name ?: "") }
    var dateMillis by rememberSaveable {
        mutableLongStateOf(existingCompetition?.date ?: System.currentTimeMillis())
    }
    var location by rememberSaveable { mutableStateOf(existingCompetition?.location ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingCompetition?.notes ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                if (existingCompetition == null) {
                    tr("Create Competition", "إنشاء بطولة")
                } else {
                    tr("Edit Competition", "تعديل البطولة")
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(tr("Competition Name", "اسم البطولة")) },
                    placeholder = {
                        Text(tr("e.g. Giza Summer Championship 2026", "مثال: بطولة الصيف 2026"))
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("competition_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            "Date: ${SwimTimeUtils.formatDateShort(dateMillis, lang)}",
                            "التاريخ: ${SwimTimeUtils.formatDateShort(dateMillis, lang)}"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(onClick = {
                        focusManager.clearFocus()
                        showDatePicker = true
                    }) {
                        Text(tr("Change", "تغيير"))
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(tr("Location / Pool", "المكان / المسبح")) },
                    placeholder = { Text(tr("e.g. Olympic Aquatic Center", "مثال: مجمع السباحة الأولمبي")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("competition_location_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Notes", "ملاحظات")) },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("competition_notes_input")
                )
            }
        },
        confirmButton = {
            val emptyNameErr = tr("Please enter a competition name", "يرجى إدخال اسم البطولة")
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (name.isBlank()) {
                        nameError = emptyNameErr
                        return@Button
                    }
                    onSave(
                        Competition(
                            id = existingCompetition?.id ?: 0L,
                            name = name.trim(),
                            date = dateMillis,
                            location = location.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_competition_button")
            ) {
                Text(tr("Save", "حفظ"))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) {
                Text(tr("Cancel", "إلغاء"))
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text(tr("OK", "موافق"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(tr("Cancel", "إلغاء"))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompetitionResultFormDialog(
    competitionId: Long,
    existingResult: CompetitionResult?,
    defaultPoolLength: PoolLength,
    onDismiss: () -> Unit,
    onSave: (CompetitionResult) -> Unit
) {
    val lang = LocalAppLanguage.current
    var distance by rememberSaveable { mutableStateOf(existingResult?.distance ?: SwimDistance.D50M) }
    var stroke by rememberSaveable { mutableStateOf(existingResult?.stroke ?: Stroke.FREESTYLE) }
    var poolLength by rememberSaveable { mutableStateOf(existingResult?.poolLength ?: defaultPoolLength) }
    var status by rememberSaveable { mutableStateOf(existingResult?.status ?: ResultStatus.FINISHED) }
    var heatText by rememberSaveable { mutableStateOf(existingResult?.heat?.toString() ?: "") }
    var laneText by rememberSaveable { mutableStateOf(existingResult?.lane?.toString() ?: "") }
    var placeText by rememberSaveable { mutableStateOf(existingResult?.place?.toString() ?: "") }
    var seedTimeText by rememberSaveable {
        mutableStateOf(existingResult?.seedTimeMillis?.let { SwimTimeUtils.formatSwimTime(it) } ?: "")
    }
    var resultTimeText by rememberSaveable {
        mutableStateOf(
            existingResult?.resultTimeMillis?.takeIf { it > 0L }?.let { SwimTimeUtils.formatSwimTime(it) } ?: ""
        )
    }
    var notes by rememberSaveable { mutableStateOf(existingResult?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        title = {
            Text(
                if (existingResult == null) {
                    tr("Add Competition Event", "إضافة سباق للبطولة")
                } else {
                    tr("Edit Competition Event", "تعديل سباق البطولة")
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    tr("Distance", "المسافة"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwimDistance.entries.forEach { d ->
                        FilterChip(
                            selected = distance == d,
                            onClick = { distance = d },
                            label = { Text(d.localizedLabel(lang)) }
                        )
                    }
                }

                Text(
                    tr("Stroke", "نوع السباحة"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Stroke.entries.forEach { s ->
                        FilterChip(
                            selected = stroke == s,
                            onClick = { stroke = s },
                            label = { Text(s.localizedShortName(lang)) }
                        )
                    }
                }

                Text(
                    tr("Pool Length", "طول المسبح"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoolLength.entries.forEach { p ->
                        FilterChip(
                            selected = poolLength == p,
                            onClick = { poolLength = p },
                            label = { Text(p.localizedLabel(lang)) }
                        )
                    }
                }

                Text(
                    tr("Result Status", "حالة النتيجة"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResultStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.localizedBadge(lang)) },
                            modifier = Modifier.testTag("comp_status_chip_${st.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = heatText,
                        onValueChange = { heatText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Heat", "التصفية")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_heat_input")
                    )
                    OutlinedTextField(
                        value = laneText,
                        onValueChange = { laneText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Lane", "الحارة")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_lane_input")
                    )
                    OutlinedTextField(
                        value = placeText,
                        onValueChange = { placeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Place", "المركز")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_place_input")
                    )
                }

                OutlinedTextField(
                    value = seedTimeText,
                    onValueChange = { seedTimeText = it },
                    label = { Text(tr("Seed Time (Optional, e.g. 37.50)", "زمن التسجيل (اختياري، مثل 37.50)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comp_seed_time_input")
                )

                OutlinedTextField(
                    value = resultTimeText,
                    onValueChange = {
                        resultTimeText = it
                        errorText = null
                    },
                    label = {
                        Text(
                            if (status == ResultStatus.FINISHED) {
                                tr("Official Result Time (e.g. 37.04)", "زمن النتيجة الرسمي (مثل 37.04)")
                            } else {
                                tr(
                                    "Result Time (Optional for ${status.localizedBadge(lang)})",
                                    "زمن النتيجة (اختياري لـ ${status.localizedBadge(lang)})"
                                )
                            }
                        )
                    },
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comp_result_time_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Notes", "ملاحظات")) },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val invalidTimeErr = tr(
                "Enter a valid finish time (e.g. 37.04 or 1:23.00)",
                "أدخل زمن نهاية صحيح (مثل 37.04 أو 1:23.00)"
            )
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val parsedResult = SwimTimeUtils.parseSwimTime(resultTimeText)
                    if (status == ResultStatus.FINISHED && (parsedResult == null || parsedResult <= 0L)) {
                        errorText = invalidTimeErr
                        return@Button
                    }
                    val parsedSeed = if (seedTimeText.isNotBlank()) {
                        SwimTimeUtils.parseSwimTime(seedTimeText)
                    } else null

                    onSave(
                        CompetitionResult(
                            id = existingResult?.id ?: 0L,
                            competitionId = competitionId,
                            swimRecordId = existingResult?.swimRecordId,
                            distance = distance,
                            stroke = stroke,
                            poolLength = poolLength,
                            startType = StartType.DIVE,
                            heat = heatText.toIntOrNull(),
                            lane = laneText.toIntOrNull(),
                            place = placeText.toIntOrNull(),
                            seedTimeMillis = parsedSeed,
                            resultTimeMillis = parsedResult,
                            status = status,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_competition_event_button")
            ) {
                Text(tr("Save Event", "حفظ السباق"))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus()
                onDismiss()
            }) {
                Text(tr("Cancel", "إلغاء"))
            }
        }
    )
}
