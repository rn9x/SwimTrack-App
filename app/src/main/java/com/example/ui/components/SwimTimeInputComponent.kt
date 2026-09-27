package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.SwimTimeUtils
import com.example.ui.theme.SwimTimeTextStyles
import com.example.ui.theme.tr

/**
 * Specialized swimming time input supporting direct keyboard time entry
 * (e.g. "33.20", "1:23.45", "2:01.30", "15.80") with live digital stopwatch validation.
 */
@Composable
fun SwimTimeInputSection(
    timeInputText: String,
    onTimeInputChange: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val parsedMillis = SwimTimeUtils.parseSwimTime(timeInputText)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = tr("Swim Time", "زمن السباحة"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr("Minutes : Seconds . Hundredths", "دقائق : ثوانٍ . أجزاء من الثانية"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Large Digital Stopwatch Readout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (timeInputText.isBlank()) "00.00" else timeInputText,
                        style = SwimTimeTextStyles.HeroDisplay,
                        color = if (timeInputText.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("time_display_readout")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (parsedMillis != null) {
                            tr(
                                "Recorded as ${SwimTimeUtils.formatSwimTime(parsedMillis)} (${parsedMillis} ms)",
                                "يُحفظ كـ ${SwimTimeUtils.formatSwimTime(parsedMillis)} (${parsedMillis} مللي ثانية)"
                            )
                        } else if (timeInputText.isBlank()) {
                            tr(
                                "Examples: 33.20 • 1:23.45 • 2:01.30 • 15.80",
                                "أمثلة: 33.20 • 1:23.45 • 2:01.30 • 15.80"
                            )
                        } else {
                            tr(
                                "Invalid format — use SS.cc or M:SS.cc (seconds < 60)",
                                "صيغة غير صحيحة — استخدم SS.cc أو M:SS.cc (الثواني أقل من 60)"
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (timeInputText.isNotBlank() && parsedMillis == null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Keyboard Text Field
            OutlinedTextField(
                value = timeInputText,
                onValueChange = { newText ->
                    onTimeInputChange(newText)
                },
                label = {
                    Text(
                        tr(
                            "Time (e.g. 33.20 or 1:23.45)",
                            "الزمن (مثال: 33.20 أو 1:23.45)"
                        )
                    )
                },
                placeholder = { Text("33.20") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = if (timeInputText.isNotBlank()) {
                    {
                        IconButton(onClick = { onTimeInputChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = tr("Clear time", "مسح الزمن")
                            )
                        }
                    }
                } else null,
                singleLine = true,
                isError = errorMessage != null,
                supportingText = if (errorMessage != null) {
                    { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("swim_time_input")
            )
        }
    }
}
