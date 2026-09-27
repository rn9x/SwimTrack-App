package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.NewPbCelebration
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.SessionType
import com.example.domain.model.SwimRecord
import com.example.domain.model.SwimTimeUtils
import com.example.domain.model.TimeFormatPreference
import com.example.ui.theme.ImprovementGreenContainerDark
import com.example.ui.theme.ImprovementGreenContainerLight
import com.example.ui.theme.ImprovementGreenDark
import com.example.ui.theme.ImprovementGreenLight
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PbGoldContainerDark
import com.example.ui.theme.PbGoldContainerLight
import com.example.ui.theme.PbGoldDark
import com.example.ui.theme.PbGoldLight
import com.example.ui.theme.StatusDisqualifiedContainerDark
import com.example.ui.theme.StatusDisqualifiedContainerLight
import com.example.ui.theme.StatusDisqualifiedRed
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr

@Composable
fun PbBadge(modifier: Modifier = Modifier, text: String? = null) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) PbGoldContainerDark else PbGoldContainerLight
    val contentColor = if (isDark) PbGoldDark else PbGoldLight
    val displayText = text ?: tr("PB", "رقم قياسي")

    Surface(
        modifier = modifier.testTag("pb_badge"),
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = tr("Personal Best", "أفضل رقم شخصي"),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusBadge(status: ResultStatus, modifier: Modifier = Modifier) {
    if (status == ResultStatus.FINISHED) return
    val lang = LocalAppLanguage.current
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) StatusDisqualifiedContainerDark else StatusDisqualifiedContainerLight
    val contentColor = StatusDisqualifiedRed

    Surface(
        modifier = modifier.testTag("status_badge_${status.name}"),
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (status == ResultStatus.DQ) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                contentDescription = status.localizedName(lang),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.localizedBadge(lang),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PoolBadge(poolLength: PoolLength, modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = poolLength.localizedBadgeText(lang),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SessionBadge(sessionType: SessionType, modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    val containerColor = when (sessionType) {
        SessionType.COMPETITION -> MaterialTheme.colorScheme.primaryContainer
        SessionType.TIME_TRIAL -> MaterialTheme.colorScheme.tertiaryContainer
        SessionType.TRAINING -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (sessionType) {
        SessionType.COMPETITION -> MaterialTheme.colorScheme.onPrimaryContainer
        SessionType.TIME_TRIAL -> MaterialTheme.colorScheme.onTertiaryContainer
        SessionType.TRAINING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = sessionType.localizedName(lang),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwimRecordCard(
    record: SwimRecord,
    timeFormat: TimeFormatPreference,
    onCardClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("swim_record_card_${record.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = if (record.isPersonalBest) {
            BorderStroke(1.5.dp, if (isSystemInDarkTheme()) PbGoldDark.copy(alpha = 0.6f) else PbGoldLight.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = record.localizedEventTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (record.isPersonalBest) {
                            PbBadge()
                        }
                        if (record.status != ResultStatus.FINISHED) {
                            StatusBadge(status = record.status)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SwimTimeUtils.formatDateShort(record.date, lang) +
                            (record.competitionName?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Large readable swimming time
                Text(
                    text = if (record.status == ResultStatus.FINISHED) {
                        SwimTimeUtils.formatSwimTime(record.timeMillis, timeFormat)
                    } else {
                        record.status.localizedBadge(lang)
                    },
                    style = SwimTimeTextStyles.ListPrimary,
                    color = if (record.status != ResultStatus.FINISHED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.testTag("record_time_${record.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    SessionBadge(sessionType = record.sessionType)
                    PoolBadge(poolLength = record.poolLength)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = record.startType.localizedName(lang),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (onEditClick != null || onDeleteClick != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEditClick != null) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("edit_record_${record.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = tr("Edit record", "تعديل السجل"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onDeleteClick != null) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("delete_record_${record.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = tr("Delete record", "حذف السجل"),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NewPbCelebrationDialog(
    celebration: NewPbCelebration,
    timeFormat: TimeFormatPreference,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val isDark = isSystemInDarkTheme()
    val goldColor = if (isDark) PbGoldDark else PbGoldLight
    val greenColor = if (isDark) ImprovementGreenDark else ImprovementGreenLight
    val greenBg = if (isDark) ImprovementGreenContainerDark else ImprovementGreenContainerLight

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isDark) PbGoldContainerDark else PbGoldContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = tr("New Personal Best Trophy", "كأس الرقم القياسي الجديد"),
                    tint = goldColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tr("NEW PB!", "رقم قياسي جديد!"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = goldColor,
                    modifier = Modifier.testTag("new_pb_dialog_title")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${celebration.localizedEventTitle(lang)} (${celebration.poolLength.localizedLabel(lang)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = SwimTimeUtils.formatSwimTime(celebration.newPbMillis, timeFormat),
                    style = SwimTimeTextStyles.HeroDisplay,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("new_pb_time_value")
                )

                if (celebration.previousPbMillis != null && celebration.improvementMillis != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tr("Previous:", "السابق:"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SwimTimeUtils.formatSwimTime(celebration.previousPbMillis, timeFormat),
                                    style = SwimTimeTextStyles.StatValue,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("Improvement:", "مقدار التحسن:"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = greenBg,
                                    contentColor = greenColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = SwimTimeUtils.formatAbsoluteDifference(celebration.improvementMillis, lang),
                                            style = SwimTimeTextStyles.SplitBadge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("new_pb_improvement_value")
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = tr(
                            "First personal best recorded for this event!",
                            "أول رقم قياسي شخصي مسجل في هذا السباق!"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_pb_dialog_button")
            ) {
                Text(tr("Awesome!", "رائع!"))
            }
        }
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector = Icons.Default.Pool,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_state_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.testTag("empty_state_primary_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }

            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier.testTag("empty_state_secondary_button")
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmText ?: tr("Delete", "حذف"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(tr("Cancel", "إلغاء"))
            }
        }
    )
}
