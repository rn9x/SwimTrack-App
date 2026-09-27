package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppSettings
import com.example.domain.model.CompetitionWithResults
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.StartType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.ui.components.SwimTimeInputSection
import com.example.ui.theme.tr

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRecordScreen(
    existingRecord: SwimRecord?,
    competitions: List<CompetitionWithResults>,
    settings: AppSettings,
    onSaveRecord: (SwimRecord) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    var dateMillis by rememberSaveable {
        mutableLongStateOf(existingRecord?.date ?: System.currentTimeMillis())
    }
    var timeInputText by rememberSaveable {
        mutableStateOf(
            existingRecord?.let { SwimTimeUtils.formatSwimTime(it.timeMillis) } ?: ""
        )
    }
    var selectedDistance by rememberSaveable {
        mutableStateOf(existingRecord?.distance ?: SwimDistance.D50M)
    }
    var selectedStroke by rememberSaveable {
        mutableStateOf(existingRecord?.stroke ?: Stroke.FREESTYLE)
    }
    var selectedSessionType by rememberSaveable {
        mutableStateOf(existingRecord?.sessionType ?: settings.defaultSessionType)
    }
    var selectedPoolLength by rememberSaveable {
        mutableStateOf(existingRecord?.poolLength ?: settings.defaultPoolLength)
    }
    var selectedStartType by rememberSaveable {
        mutableStateOf(existingRecord?.startType ?: StartType.DIVE)
    }
    var selectedCompetitionId by rememberSaveable {
        mutableStateOf(existingRecord?.competitionId)
    }
    var notes by rememberSaveable {
        mutableStateOf(existingRecord?.notes ?: "")
    }

    var timeError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var competitionDropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Update defaults when creating a new record and settings load
    LaunchedEffect(existingRecord) {
        if (existingRecord != null) {
            dateMillis = existingRecord.date
            timeInputText = SwimTimeUtils.formatSwimTime(existingRecord.timeMillis)
            selectedDistance = existingRecord.distance
            selectedStroke = existingRecord.stroke
            selectedSessionType = existingRecord.sessionType
            selectedPoolLength = existingRecord.poolLength
            selectedStartType = existingRecord.startType
            selectedCompetitionId = existingRecord.competitionId
            notes = existingRecord.notes
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_record_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (existingRecord == null) {
                    tr("Log Swimming Result", "تسجيل نتيجة سباحة")
                } else {
                    tr("Edit Swimming Result", "تعديل نتيجة السباحة")
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tr(
                    "Record your time accurately to automatically track PBs and progression.",
                    "سجّل وقتك بدقة لتتبع أرقامك القياسية وتطور أدائك تلقائياً."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Specialized Swimming Time Input
        item {
            SwimTimeInputSection(
                timeInputText = timeInputText,
                onTimeInputChange = {
                    timeInputText = it
                    timeError = null
                },
                errorMessage = timeError
            )
        }

        // 2. Distance Selection
        item {
            FormSectionCard(title = tr("Distance", "المسافة")) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwimDistance.entries.forEach { distance ->
                        val isSelected = selectedDistance == distance
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDistance = distance },
                            label = {
                                Text(
                                    text = distance.localizedLabel(lang),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("distance_chip_${distance.label}")
                        )
                    }
                }
            }
        }

        // 3. Stroke Selection
        item {
            FormSectionCard(title = tr("Stroke", "نوع السباحة")) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Stroke.entries.forEach { stroke ->
                        val isSelected = selectedStroke == stroke
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStroke = stroke },
                            label = {
                                Text(
                                    text = stroke.localizedName(lang),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("stroke_chip_${stroke.name}")
                        )
                    }
                }
            }
        }

        // 4. Pool Length & Start Type
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormSectionCard(
                    title = tr("Pool Length", "طول المسبح"),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PoolLength.entries.forEach { pool ->
                            FilterChip(
                                selected = selectedPoolLength == pool,
                                onClick = { selectedPoolLength = pool },
                                label = { Text(pool.localizedLabel(lang)) },
                                modifier = Modifier.testTag("pool_chip_${pool.label}")
                            )
                        }
                    }
                }

                FormSectionCard(
                    title = tr("Start Type", "نوع البداية"),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartType.entries.forEach { start ->
                            FilterChip(
                                selected = selectedStartType == start,
                                onClick = { selectedStartType = start },
                                label = { Text(start.localizedShortName(lang)) },
                                modifier = Modifier.testTag("start_chip_${start.name}")
                            )
                        }
                    }
                }
            }
        }

        // 5. Session Type & Optional Competition Link
        item {
            FormSectionCard(title = tr("Session Type", "نوع الجلسة")) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SessionType.entries.forEach { session ->
                        FilterChip(
                            selected = selectedSessionType == session,
                            onClick = {
                                selectedSessionType = session
                                if (session != SessionType.COMPETITION) {
                                    selectedCompetitionId = null
                                } else if (selectedCompetitionId == null && competitions.isNotEmpty()) {
                                    val firstComp = competitions.first().competition
                                    selectedCompetitionId = firstComp.id
                                    dateMillis = firstComp.date
                                }
                            },
                            label = { Text(session.localizedName(lang)) },
                            modifier = Modifier.testTag("session_chip_${session.name}")
                        )
                    }
                }

                if (competitions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val noneText = tr("None (Standalone Record)", "بدون بطولة (سجل مستقل)")
                    val selectedCompName = competitions
                        .firstOrNull { it.competition.id == selectedCompetitionId }
                        ?.competition?.name ?: noneText

                    ExposedDropdownMenuBox(
                        expanded = competitionDropdownExpanded,
                        onExpandedChange = { competitionDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCompName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr("Competition (Optional)", "البطولة (اختياري)")) },
                            leadingIcon = {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = competitionDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = competitionDropdownExpanded,
                            onDismissRequest = { competitionDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(noneText) },
                                onClick = {
                                    selectedCompetitionId = null
                                    competitionDropdownExpanded = false
                                }
                            )
                            competitions.forEach { compWithResults ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${compWithResults.competition.name} (${SwimTimeUtils.formatDateShort(compWithResults.competition.date, lang)})")
                                    },
                                    onClick = {
                                        selectedCompetitionId = compWithResults.competition.id
                                        selectedSessionType = SessionType.COMPETITION
                                        dateMillis = compWithResults.competition.date
                                        competitionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Date Selection & Notes
        item {
            FormSectionCard(title = tr("Date & Notes", "التاريخ والملاحظات")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Swim Date", "تاريخ السباحة"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SwimTimeUtils.formatDateShort(dateMillis, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("select_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Change Date", "تغيير التاريخ"))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Notes (splits, stroke rate, feeling...)", "ملاحظات (الأزمنة الجزئية، معدل الضربات، الشعور...)")) },
                    placeholder = { Text(tr("e.g. Strong underwater dolphin kicks, felt smooth", "مثال: ضربات دولفين قوية تحت الماء، سباحة سلسة")) },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_notes_input")
                )
            }
        }

        // 7. Save & Cancel Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onCancel()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("cancel_save_record_button")
                ) {
                    Text(tr("Cancel", "إلغاء"))
                }

                val invalidTimeMessage = tr(
                    "Please enter a valid swimming time (e.g. 33.20 or 1:23.45)",
                    "يرجى إدخال زمن سباحة صحيح (مثال: 33.20 أو 1:23.45)"
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val parsedTime = SwimTimeUtils.parseSwimTime(timeInputText)
                        if (parsedTime == null || parsedTime <= 0L) {
                            timeError = invalidTimeMessage
                            return@Button
                        }
                        val record = SwimRecord(
                            id = existingRecord?.id ?: 0L,
                            date = dateMillis,
                            timeMillis = parsedTime,
                            distance = selectedDistance,
                            stroke = selectedStroke,
                            sessionType = selectedSessionType,
                            poolLength = selectedPoolLength,
                            startType = selectedStartType,
                            competitionId = selectedCompetitionId,
                            status = existingRecord?.status ?: ResultStatus.FINISHED,
                            notes = notes.trim(),
                            createdAt = existingRecord?.createdAt ?: System.currentTimeMillis()
                        )
                        onSaveRecord(record)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("save_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingRecord == null) {
                            tr("Save Record", "حفظ السجل")
                        } else {
                            tr("Update Record", "تحديث السجل")
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
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
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}
