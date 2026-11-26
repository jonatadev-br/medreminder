package com.jonataprezende.medreminder.ui.addmed

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.alarm.AlarmScheduler
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.entity.ScheduleType
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.components.CTAButton
import com.jonataprezende.medreminder.ui.components.getMedicationIcon
import com.jonataprezende.medreminder.ui.components.parseColor
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    repository: MedRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alarmScheduler = remember { AlarmScheduler.getInstance(context) }

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf(ScheduleType.FIXED_TIME) }
    var timesText by remember { mutableStateOf("") }
    var intervalHours by remember { mutableStateOf(8) }
    var durationType by remember { mutableStateOf(DurationType.CONTINUOUS) }
    var daysText by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#0E9F8A") }
    var iconName by remember { mutableStateOf("pill") }
    var saving by remember { mutableStateOf(false) }

    val colorOptions = listOf(
        "#0E9F8A", "#2563EB", "#7C3AED", "#E0453A",
        "#F5A623", "#1B7A43", "#8D5A3B", "#EC4899"
    )
    val iconOptions = listOf("pill", "capsule", "drops", "syringe", "inhaler", "patch")
    val intervalOptions = listOf(6, 8, 12)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = stringResource(R.string.add_med_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.button_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormLabel(stringResource(R.string.add_med_step_basic))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_name)) },
                placeholder = { Text(stringResource(R.string.hint_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text(stringResource(R.string.label_dosage)) },
                placeholder = { Text(stringResource(R.string.hint_dosage)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                placeholder = { Text(stringResource(R.string.hint_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            FormLabel(stringResource(R.string.add_med_step_schedule))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectChip(
                    label = stringResource(R.string.schedule_type_fixed),
                    selected = scheduleType == ScheduleType.FIXED_TIME,
                    onClick = { scheduleType = ScheduleType.FIXED_TIME }
                )
                SelectChip(
                    label = stringResource(R.string.schedule_type_interval),
                    selected = scheduleType == ScheduleType.INTERVAL,
                    onClick = { scheduleType = ScheduleType.INTERVAL }
                )
            }

            if (scheduleType == ScheduleType.FIXED_TIME) {
                OutlinedTextField(
                    value = timesText,
                    onValueChange = { timesText = it },
                    label = { Text(stringResource(R.string.label_fixed_times)) },
                    placeholder = { Text(stringResource(R.string.hint_fixed_times)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                FormLabel(stringResource(R.string.label_interval))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervalOptions.forEach { hours ->
                        SelectChip(
                            label = "A cada ${hours}h",
                            selected = intervalHours == hours,
                            onClick = { intervalHours = hours }
                        )
                    }
                }
            }

            FormLabel(stringResource(R.string.label_duration))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectChip(
                    label = stringResource(R.string.duration_continuous),
                    selected = durationType == DurationType.CONTINUOUS,
                    onClick = { durationType = DurationType.CONTINUOUS }
                )
                SelectChip(
                    label = stringResource(R.string.duration_days),
                    selected = durationType == DurationType.DAYS,
                    onClick = { durationType = DurationType.DAYS }
                )
            }

            if (durationType == DurationType.DAYS) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { input -> daysText = input.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.hint_days)) },
                    placeholder = { Text("Ex: 10") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            FormLabel(stringResource(R.string.add_med_step_appearance))

            Text(
                text = stringResource(R.string.label_color),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(colorOptions) { color ->
                    ColorCircle(
                        colorHex = color,
                        selected = colorHex == color,
                        onClick = { colorHex = color }
                    )
                }
            }

            Text(
                text = stringResource(R.string.label_icon),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(iconOptions) { icon ->
                    IconCircle(
                        iconName = icon,
                        colorHex = colorHex,
                        selected = iconName == icon,
                        onClick = { iconName = icon }
                    )
                }
            }

            CTAButton(
                text = stringResource(R.string.button_save),
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                onClick = {
                    val timeList = if (scheduleType == ScheduleType.FIXED_TIME) {
                        parseTimesInput(timesText)
                    } else emptyList()
                    val days = if (durationType == DurationType.DAYS) daysText.toIntOrNull() else null
                    val nameValid = name.isNotBlank() && dosage.isNotBlank()
                    val timesValid = scheduleType != ScheduleType.FIXED_TIME || timeList.isNotEmpty()
                    val daysValid = durationType != DurationType.DAYS || (days != null && days > 0)

                    when {
                        !nameValid -> toast(context, context.getString(R.string.toast_fill_required))
                        !timesValid -> toast(context, context.getString(R.string.toast_invalid_time))
                        !daysValid -> toast(context, context.getString(R.string.toast_invalid_days))
                        else -> {
                            saving = true
                            scope.launch {
                                saveMedication(
                                    repository = repository,
                                    alarmScheduler = alarmScheduler,
                                    name = name,
                                    dosage = dosage,
                                    notes = notes,
                                    scheduleType = scheduleType,
                                    timeList = timeList,
                                    intervalHours = intervalHours,
                                    days = days,
                                    colorHex = colorHex,
                                    iconName = iconName
                                )
                                saving = false
                                onSaved()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.08.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun ColorCircle(colorHex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(parseColor(colorHex))
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun IconCircle(iconName: String, colorHex: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(parseColor(colorHex))
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = shape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getMedicationIcon(iconName),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun parseTimesInput(input: String): List<String> {
    return input
        .split(",", ";", " ")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull()
                val minute = parts[1].toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    String.format("%02d:%02d", hour, minute)
                } else null
            } else null
        }
}

private suspend fun saveMedication(
    repository: MedRepository,
    alarmScheduler: AlarmScheduler,
    name: String,
    dosage: String,
    notes: String,
    scheduleType: ScheduleType,
    timeList: List<String>,
    intervalHours: Int,
    days: Int?,
    colorHex: String,
    iconName: String
) {
    val medication = Medication(
        name = name.trim(),
        dosage = dosage.trim(),
        notes = notes.trim(),
        colorHex = colorHex,
        iconRes = iconName
    )
    val medicationId = repository.insertMedication(medication)

    val startDate = System.currentTimeMillis()
    val endDate = if (days != null) startDate + days * 24L * 60 * 60 * 1000 else 0L
    val timesJson = if (scheduleType == ScheduleType.FIXED_TIME) JSONArray(timeList).toString() else "[]"

    val schedule = Schedule(
        medicationId = medicationId,
        type = scheduleType,
        timesJson = timesJson,
        intervalHours = if (scheduleType == ScheduleType.INTERVAL) intervalHours else 0,
        startDate = startDate,
        endDate = endDate
    )
    repository.insertSchedule(schedule)

    val savedSchedule = repository.getActiveSchedulesByMedication(medicationId).firstOrNull()
    if (savedSchedule != null) {
        alarmScheduler.rescheduleForSchedule(savedSchedule)
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

enum class DurationType {
    CONTINUOUS,
    DAYS
}
