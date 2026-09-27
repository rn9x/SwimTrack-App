package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.CompetitionWithResults
import com.example.domain.model.PersonalBestSummary
import com.example.domain.model.SessionType
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PbBadge
import com.example.ui.components.PoolBadge
import com.example.ui.components.SwimRecordCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PbGoldContainerDark
import com.example.ui.theme.PbGoldContainerLight
import com.example.ui.theme.PbGoldDark
import com.example.ui.theme.PbGoldLight
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr

@Composable
fun HomeScreen(
    allRecords: List<SwimRecord>,
    personalBests: List<PersonalBestSummary>,
    competitions: List<CompetitionWithResults>,
    settings: AppSettings,
    onQuickAddClick: () -> Unit,
    onViewAllRecordsClick: () -> Unit,
    onEventClick: (SwimDistance, Stroke) -> Unit,
    onEditRecordClick: (Long) -> Unit,
    onDeleteRecordClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onLoadDemoDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = settings.language
    var recordToDelete by remember { mutableStateOf<SwimRecord?>(null) }

    val totalRecords = allRecords.size
    val totalCompetitions = competitions.size
    val totalTrainingRecords = remember(allRecords) {
        allRecords.count { it.sessionType == SessionType.TRAINING }
    }
    val recentRecords = remember(allRecords) {
        allRecords.take(5)
    }
    val mostRecentPb = remember(personalBests) {
        personalBests.maxByOrNull { it.record.date }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Aquatic Hero Header Card with Quick Add
        item {
            HeroBannerCard(
                onQuickAddClick = onQuickAddClick,
                onSettingsClick = onSettingsClick
            )
        }

        // 2. Summary Counters (Total Records, Total Competitions, Total Training Records)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    label = tr("Total Records", "إجمالي السجلات"),
                    value = totalRecords.toString(),
                    icon = Icons.Default.Timer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_total_records")
                )
                MetricStatCard(
                    label = tr("Competitions", "البطولات"),
                    value = totalCompetitions.toString(),
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_total_competitions")
                )
                MetricStatCard(
                    label = tr("Training", "التدريبات"),
                    value = totalTrainingRecords.toString(),
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_total_training")
                )
            }
        }

        if (allRecords.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Pool,
                    title = tr("No swimming records yet", "لا توجد سجلات سباحة بعد"),
                    subtitle = tr(
                        "Add your first result to start tracking your progress.",
                        "أضف أول نتيجة لك لبدء تتبع تطورك وأرقامك القياسية."
                    ),
                    actionLabel = tr("Add Record", "إضافة سجل"),
                    onActionClick = onQuickAddClick,
                    secondaryActionLabel = tr("Load Demo Sample Data", "تحميل بيانات تجريبية"),
                    onSecondaryActionClick = onLoadDemoDataClick
                )
            }
        } else {
            // 3. Personal Bests Summary Card
            item {
                PersonalBestsOverviewCard(
                    pbCount = personalBests.size,
                    mostRecentPb = mostRecentPb,
                    settings = settings,
                    onEventClick = onEventClick
                )
            }

            // 4. Best Times Carousel / Grid for Commonly Used Events
            if (personalBests.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Best Times", "أفضل الأوقات"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("${personalBests.size} Events", "${personalBests.size} سباقات"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(personalBests, key = { "${it.eventKey.distance}_${it.eventKey.stroke}" }) { pbSummary ->
                                BestTimeEventCard(
                                    pbSummary = pbSummary,
                                    settings = settings,
                                    onClick = {
                                        onEventClick(pbSummary.eventKey.distance, pbSummary.eventKey.stroke)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recent Records (Last 5 records)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tr("Recent Records", "أحدث السجلات"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onViewAllRecordsClick,
                        modifier = Modifier.testTag("view_all_records_button")
                    ) {
                        Text(tr("View All", "عرض الكل"))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            items(recentRecords, key = { it.id }) { record ->
                SwimRecordCard(
                    record = record,
                    timeFormat = settings.timeFormatPreference,
                    onCardClick = { onEventClick(record.distance, record.stroke) },
                    onEditClick = { onEditRecordClick(record.id) },
                    onDeleteClick = { recordToDelete = record }
                )
            }
        }
    }

    recordToDelete?.let { record ->
        val formattedTime = SwimTimeUtils.formatSwimTime(record.timeMillis, settings.timeFormatPreference)
        val formattedDate = SwimTimeUtils.formatDateShort(record.date, lang)
        ConfirmDeleteDialog(
            title = tr("Delete Swim Record?", "حذف سجل السباحة؟"),
            message = tr(
                "Remove ${record.eventTitle} ($formattedTime) from $formattedDate?",
                "هل تريد حذف سجل ${record.localizedEventTitle(lang)} ($formattedTime) بتاريخ $formattedDate؟"
            ),
            onConfirm = { onDeleteRecordClick(record.id) },
            onDismiss = { recordToDelete = null }
        )
    }
}

@Composable
private fun HeroBannerCard(
    onQuickAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_pool_hero),
                contentDescription = "Olympic swimming pool lanes",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xF2052C65),
                                Color(0xCC0369A1),
                                Color(0x990284C7)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_swimtrack_logo),
                            contentDescription = "SwimTrack Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color.White)) {
                                        append("Swim")
                                    }
                                    withStyle(SpanStyle(color = Color(0xFF38BDF8))) {
                                        append("Track")
                                    }
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Swim. Track. Improve.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE0F2FE)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("100% Offline Performance Log", "سجل أداء شخصي يعمل 100% بدون إنترنت"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    )

                    Button(
                        onClick = onQuickAddClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF052C65)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("home_quick_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Quick Add", "إضافة سريعة"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    label: String,
    value: String,
    icon: ImageVector,
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = SwimTimeTextStyles.CardLarge,
                color = MaterialTheme.colorScheme.onSurface
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
private fun PersonalBestsOverviewCard(
    pbCount: Int,
    mostRecentPb: PersonalBestSummary?,
    settings: AppSettings,
    onEventClick: (SwimDistance, Stroke) -> Unit
) {
    val lang = settings.language
    val isDark = isSystemInDarkTheme()
    val goldBg = if (isDark) PbGoldContainerDark else PbGoldContainerLight
    val goldColor = if (isDark) PbGoldDark else PbGoldLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("personal_bests_overview_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, goldColor.copy(alpha = 0.45f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(goldBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tr("Personal Bests", "الأرقام القياسية الشخصية"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "$pbCount Events with Official PB",
                                "$pbCount سباقات برقم قياسي مسجل"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = goldBg,
                    contentColor = goldColor,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = tr("$pbCount PBs", "$pbCount رقم قياسي"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (mostRecentPb != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onEventClick(mostRecentPb.eventKey.distance, mostRecentPb.eventKey.stroke)
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("MOST RECENT PB", "أحدث رقم قياسي"),
                                style = MaterialTheme.typography.labelSmall,
                                color = goldColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mostRecentPb.eventKey.localizedDisplayName(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${SwimTimeUtils.formatDateShort(mostRecentPb.record.date, lang)} • ${mostRecentPb.record.poolLength.localizedBadgeText(lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SwimTimeUtils.formatSwimTime(
                                    mostRecentPb.record.timeMillis,
                                    settings.timeFormatPreference
                                ),
                                style = SwimTimeTextStyles.CardLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            mostRecentPb.improvementComparison?.let { comp ->
                                if (comp.isImprovement) {
                                    val diffStr = SwimTimeUtils.formatAbsoluteDifference(comp.differenceMillis, lang)
                                    Text(
                                        text = tr("Improved by $diffStr", "تحسّن بمقدار $diffStr"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestTimeEventCard(
    pbSummary: PersonalBestSummary,
    settings: AppSettings,
    onClick: () -> Unit
) {
    val lang = settings.language
    Card(
        modifier = Modifier
            .width(185.dp)
            .clickable(onClick = onClick)
            .testTag("best_time_card_${pbSummary.eventKey.distance.name}_${pbSummary.eventKey.stroke.name}"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PbBadge()
                PoolBadge(poolLength = pbSummary.record.poolLength)
            }

            Text(
                text = pbSummary.eventKey.localizedDisplayName(lang),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = SwimTimeUtils.formatSwimTime(
                    pbSummary.record.timeMillis,
                    settings.timeFormatPreference
                ),
                style = SwimTimeTextStyles.CardLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = SwimTimeUtils.formatDateShort(pbSummary.record.date, lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
