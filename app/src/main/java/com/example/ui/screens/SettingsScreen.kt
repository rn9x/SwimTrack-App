package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.PoolLength
import com.example.domain.model.SessionType
import com.example.domain.model.ThemePreference
import com.example.domain.model.TimeFormatPreference
import com.example.ui.theme.tr
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateTheme: (ThemePreference) -> Unit,
    onUpdateTimeFormat: (TimeFormatPreference) -> Unit,
    onUpdateDefaultPool: (PoolLength) -> Unit,
    onUpdateDefaultSession: (SessionType) -> Unit,
    onUpdateCompetitionOnlyPb: (Boolean) -> Unit,
    onExportJsonSuspend: suspend () -> String,
    onImportJson: (String) -> Unit,
    onLoadDemoData: () -> Unit,
    onDeleteAllData: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lang = settings.language

    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var exportedJsonPreview by remember { mutableStateOf<String?>(null) }
    var showPasteJsonImportDialog by remember { mutableStateOf(false) }
    var pasteJsonText by remember { mutableStateOf("") }

    // SAF File Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = onExportJsonSuspend()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    onShowMessage(
                        tr(
                            lang,
                            "Backup exported successfully to JSON file.",
                            "تم تصدير النسخة الاحتياطية إلى ملف JSON بنجاح."
                        )
                    )
                } catch (e: Exception) {
                    onShowMessage(
                        tr(
                            lang,
                            "Export failed: ${e.localizedMessage ?: "Unable to write file"}",
                            "فشل التصدير: تعذر كتابة الملف"
                        )
                    )
                }
            }
        }
    }

    // SAF File Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                    if (!content.isNullOrBlank()) {
                        onImportJson(content)
                    } else {
                        onShowMessage(
                            tr(lang, "Selected file is empty.", "الملف المختار فارغ.")
                        )
                    }
                } catch (e: Exception) {
                    onShowMessage(
                        tr(
                            lang,
                            "Import failed: ${e.localizedMessage ?: "Unable to read file"}",
                            "فشل الاستيراد: تعذر قراءة الملف"
                        )
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Back", "رجوع")
                    )
                }
                Column {
                    Text(
                        text = tr("Settings & Data Backup", "الإعدادات والنسخ الاحتياطي"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Customize language, theme, time format, defaults, and offline backups",
                            "تخصيص اللغة، المظهر، تنسيق الوقت، الإعدادات الافتراضية، والنسخ الاحتياطي"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 0. Language Choice (English default on install / Arabic)
        item {
            SettingsCard(title = tr("Language / اللغة", "اللغة / Language")) {
                Text(
                    text = tr(
                        "Choose your preferred app language (English is the default).",
                        "اختر لغة التطبيق المفضلة لديك (الإنجليزية هي اللغة الافتراضية)."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLanguage.entries.forEach { languageOption ->
                        val isSelected = settings.language == languageOption
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateLanguage(languageOption) },
                            label = {
                                Text(
                                    text = if (languageOption == AppLanguage.ENGLISH) {
                                        "English"
                                    } else {
                                        "العربية (Arabic)"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                            modifier = Modifier.testTag("language_chip_${languageOption.name}")
                        )
                    }
                }
            }
        }

        // 1. Theme Preference (System, Light, Dark)
        item {
            SettingsCard(title = tr("Theme", "المظهر")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.themePreference == theme,
                            onClick = { onUpdateTheme(theme) },
                            label = { Text(theme.localizedName(lang)) },
                            modifier = Modifier.testTag("theme_chip_${theme.name}")
                        )
                    }
                }
            }
        }

        // 2. Time Format (Minutes + seconds vs Seconds)
        item {
            SettingsCard(title = tr("Time Format", "تنسيق الوقت")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeFormatPreference.entries.forEach { format ->
                        FilterChip(
                            selected = settings.timeFormatPreference == format,
                            onClick = { onUpdateTimeFormat(format) },
                            label = { Text("${format.localizedName(lang)} (${format.example})") },
                            modifier = Modifier.testTag("time_format_chip_${format.name}")
                        )
                    }
                }
            }
        }

        // 3. Default Pool & Default Session Type
        item {
            SettingsCard(title = tr("Recording Defaults", "الإعدادات الافتراضية للتسجيل")) {
                Text(
                    text = tr("Default Pool Length", "طول المسبح الافتراضي"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoolLength.entries.forEach { pool ->
                        FilterChip(
                            selected = settings.defaultPoolLength == pool,
                            onClick = { onUpdateDefaultPool(pool) },
                            label = { Text(pool.localizedCourseLabel(lang)) },
                            modifier = Modifier.testTag("default_pool_chip_${pool.label}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tr("Default Session Type", "نوع الجلسة الافتراضي"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionType.entries.forEach { session ->
                        FilterChip(
                            selected = settings.defaultSessionType == session,
                            onClick = { onUpdateDefaultSession(session) },
                            label = { Text(session.localizedName(lang)) },
                            modifier = Modifier.testTag("default_session_chip_${session.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Competition-Only PBs", "احتساب الأرقام القياسية من البطولات فقط"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(
                                "When off, both Training and Competition valid times count toward Personal Bests.",
                                "عند الإيقاف، تُحتسب الأوقات الصحيحة من التدريب والبطولات ضمن الأرقام القياسية الشخصية."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.competitionOnlyPb,
                        onCheckedChange = onUpdateCompetitionOnlyPb,
                        modifier = Modifier.testTag("competition_only_pb_switch")
                    )
                }
            }
        }

        // 4. Data Export, Import, Demo Data & Delete All Data
        item {
            SettingsCard(title = tr("Local Data & Backup (Offline JSON)", "البيانات المحلية والنسخ الاحتياطي (JSON)")) {
                Text(
                    text = tr(
                        "Export all swimming records and competitions to a local JSON file or restore from an existing backup.",
                        "قم بتصدير جميع سجلات السباحة والبطولات إلى ملف JSON محلي أو استعادتها من نسخة احتياطية."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportLauncher.launch("swimtrack_backup.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_data_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Export Data", "تصدير البيانات"))
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_data_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Import Data", "استيراد البيانات"))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                exportedJsonPreview = onExportJsonSuspend()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_json_backup_button")
                    ) {
                        Text(tr("View Backup JSON", "عرض ملف JSON"))
                    }

                    TextButton(
                        onClick = {
                            pasteJsonText = ""
                            showPasteJsonImportDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paste_json_import_button")
                    ) {
                        Text(tr("Paste JSON Backup", "لصق نسخة JSON"))
                    }
                }

                OutlinedButton(
                    onClick = onLoadDemoData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("load_demo_data_button")
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Load Demo Sample Data (Optional)", "تحميل بيانات تجريبية (اختياري)"))
                }

                Button(
                    onClick = { showDeleteAllConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Delete All Data", "حذف جميع البيانات"))
                }
            }
        }

        // 5. About Section
        item {
            SettingsCard(title = tr("About", "حول التطبيق")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_swimtrack_logo),
                        contentDescription = "SwimTrack Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = tr("SwimTrack — Version 1.0.0", "SwimTrack — الإصدار 1.0.0"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Swim. Track. Improve.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = tr(
                        "SwimTrack is a personal offline-first swimming statistics and performance tracking application. All training sessions, competitions, personal bests, and progression charts are stored privately on your device.",
                        "تطبيق SwimTrack هو تطبيق شخصي يعمل بدون إنترنت لتتبع إحصائيات ونتائج السباحة. يتم حفظ جميع التدريبات والبطولات والأرقام القياسية الشخصية ومنحنيات التطور محلياً بخصوصية تامة على جهازك."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text(tr("Delete All Swimming Data?", "حذف جميع بيانات السباحة؟")) },
            text = {
                Text(
                    tr(
                        "WARNING: This action will permanently delete all swimming records, personal bests, competitions, and competition results from your device. Consider exporting a JSON backup first.",
                        "تحذير: سيؤدي هذا الإجراء إلى حذف جميع سجلات السباحة والأرقام القياسية والبطولات ونتائج السباقات نهائياً من جهازك. ننصحك بتصدير نسخة احتياطية JSON أولاً."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_all_data_button")
                ) {
                    Text(tr("Delete Everything", "حذف الكل نهائياً"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text(tr("Cancel", "إلغاء"))
                }
            }
        )
    }

    exportedJsonPreview?.let { json ->
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            title = { Text(tr("Exported JSON Backup", "النسخة الاحتياطية JSON")) },
            text = {
                OutlinedTextField(
                    value = json,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exported_json_text_field")
                )
            },
            confirmButton = {
                Button(onClick = { exportedJsonPreview = null }) {
                    Text(tr("Done", "تم"))
                }
            }
        )
    }

    if (showPasteJsonImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonImportDialog = false },
            title = { Text(tr("Import JSON Backup", "استيراد نسخة احتياطية JSON")) },
            text = {
                OutlinedTextField(
                    value = pasteJsonText,
                    onValueChange = { pasteJsonText = it },
                    label = { Text(tr("Paste SwimTrack JSON here", "الصق كود JSON الخاص بـ SwimTrack هنا")) },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_json_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportJson(pasteJsonText)
                        showPasteJsonImportDialog = false
                    },
                    modifier = Modifier.testTag("confirm_paste_json_import_button")
                ) {
                    Text(tr("Import", "استيراد"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonImportDialog = false }) {
                    Text(tr("Cancel", "إلغاء"))
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
