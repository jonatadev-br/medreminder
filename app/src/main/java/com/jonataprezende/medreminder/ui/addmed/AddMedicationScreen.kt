package com.jonataprezende.medreminder.ui.addmed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.entity.ScheduleType
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.components.CommonComponents.CTAButton
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedReminderCard
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedicationColorCircle
import com.jonataprezende.medreminder.ui.components.CommonComponents.SectionHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    repository: MedRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    
    // Step 1: Basic info
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Step 2: Schedule
    var scheduleType by remember { mutableStateOf(ScheduleType.FIXED_TIME) }
    var fixedTimes by remember { mutableStateOf("") }
    var intervalHours by remember { mutableStateOf(8) }
    var durationType by remember { mutableStateOf(DurationType.CONTINUOUS) }
    var durationDays by remember { mutableStateOf("") }
    
    // Step 3: Appearance
    var selectedColor by remember { mutableStateOf("#0E9F8A") }
    var selectedIcon by remember { mutableStateOf("pill") }
    
    val colorOptions = listOf("#0E9F8A", "#2563EB", "#7C3AED", "#E0453A", "#F5A623", "#1B7A43", "#8D5A3B", "#EC4899")
    val iconOptions = listOf("pill", "capsule", "drops", "syringe", "inhaler", "patch")
    val intervalOptions = listOf(6 to "A cada 6 horas", 8 to "A cada 8 horas", 12 to "A cada 12 horas")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(text = stringResource(R.string.add_med_title)) },
            navigationIcon = {
                IconButton(onClick = { if (step > 1) step-- else onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.button_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..3).forEach { s ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (s <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
                if (s < 3) androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            }
        }

        // Content
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            when (step) {
                1 -> StepBasicInfo(
                    name = name, onNameChange = { name = it },
                    dosage = dosage, onDosageChange = { dosage = it },
                    notes = notes, onNotesChange = { notes = it },
                    onNext = { 
                        if (name.isNotBlank() && dosage.isNotBlank()) step = 2 
                        else showToast(context, stringResource(R.string.toast_fill_required))
                    }
                )
                2 -> StepSchedule(
                    scheduleType = scheduleType, onScheduleTypeChange = { scheduleType = it },
                    fixedTimes = fixedTimes, onFixedTimesChange = { fixedTimes = it },
                    intervalHours = intervalHours, onIntervalHoursChange = { intervalHours = it },
                    durationType = durationType, onDurationTypeChange = { durationType = it },
                    durationDays = durationDays, onDurationDaysChange = { durationDays = it },
                    intervalOptions = intervalOptions,
                    onBack = { step = 1 },
                    onNext = { 
                        val valid = when (scheduleType) {
                            ScheduleType.FIXED_TIME -> fixedTimes.isNotBlank() && validateTimes(fixedTimes)
                            ScheduleType.INTERVAL -> true
                        }
                        val durationValid = durationType != DurationType.DAYS || durationDays.toIntOrNull()?.let { it > 0 } == true
                        if (valid && durationValid) step = 3
                        else if (!valid) showToast(context, stringResource(R.string.toast_invalid_time))
                        else showToast(context, stringResource(R.string.toast_invalid_days))
                    }
                )
                3 -> StepAppearance(
                    selectedColor = selectedColor, onColorChange = { selectedColor = it },
                    selectedIcon = selectedIcon, onIconChange = { selectedIcon = it },
                    colorOptions = colorOptions,
                    iconOptions = iconOptions,
                    onBack = { step = 2 },
                    onSave = {
                        saveMedication(
                            repository, context,
                            name, dosage, notes,
                            scheduleType, fixedTimes, intervalHours,
                            durationType, durationDays,
                            selectedColor, selectedIcon,
                            onSaved
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun StepBasicInfo(
    name: String, onNameChange: (String) -> Unit,
    dosage: String, onDosageChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(stringResource(R.string.add_med_step_basic))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.label_name)) },
            placeholder = { Text(stringResource(R.string.hint_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = dosage,
            onValueChange = onDosageChange,
            label = { Text(stringResource(R.string.label_dosage)) },
            placeholder = { Text(stringResource(R.string.hint_dosage)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.label_notes)) },
            placeholder = { Text(stringResource(R.string.hint_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 4
        )
        
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        CTAButton(text = stringResource(R.string.button_next), onClick = onNext)
    }
}

@Composable
fun StepSchedule(
    scheduleType: ScheduleType, onScheduleTypeChange: (ScheduleType) -> Unit,
    fixedTimes: String, onFixedTimesChange: (String) -> Unit,
    intervalHours: Int, onIntervalHoursChange: (Int) -> Unit,
    durationType: DurationType, onDurationTypeChange: (DurationType) -> Unit,
    durationDays: String, onDurationDaysChange: (String) -> Unit,
    intervalOptions: List<Pair<Int, String>>,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(stringResource(R.string.add_med_step_schedule))
        
        // Schedule type tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScheduleTab(
                label = stringResource(R.string.schedule_type_fixed),
                selected = scheduleType == ScheduleType.FIXED_TIME,
                onClick = { onScheduleTypeChange(ScheduleType.FIXED_TIME) }
            )
            ScheduleTab(
                label = stringResource(R.string.schedule_type_interval),
                selected = scheduleType == ScheduleType.INTERVAL,
                onClick = { onScheduleTypeChange(ScheduleType.INTERVAL) }
            )
        }
        
        if (scheduleType == ScheduleType.FIXED_TIME) {
            OutlinedTextField(
                value = fixedTimes,
                onValueChange = onFixedTimesChange,
                label = { Text(stringResource(R.string.label_fixed_times)) },
                placeholder = { Text(stringResource(R.string.hint_fixed_times)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Text(text = stringResource(R.string.label_interval), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(intervalOptions) { (hours, label) ->
                    IntervalChip(
                        label = label,
                        selected = intervalHours == hours,
                        onClick = { onIntervalHoursChange(hours) }
                    )
                }
            }
        }
        
        // Duration
        SectionHeader(stringResource(R.string.label_duration))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationTab(
                label = stringResource(R.string.duration_continuous),
                selected = durationType == DurationType.CONTINUOUS,
                onClick = { onDurationTypeChange(DurationType.CONTINUOUS) }
            )
            DurationTab(
                label = stringResource(R.string.duration_days),
                selected = durationType == DurationType.DAYS,
                onClick = { onDurationTypeChange(DurationType.DAYS) }
            )
        }
        
        if (durationType == DurationType.DAYS) {
            OutlinedTextField(
                value = durationDays,
                onValueChange = onDurationDaysChange,
                label = { Text(stringResource(R.string.hint_days)) },
                placeholder = { Text("Ex: 10") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }
        
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.button_back), fontWeight = FontWeight.W600)
            }
            CTAButton(text = stringResource(R.string.button_next), onClick = onNext, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StepAppearance(
    selectedColor: String, onColorChange: (String) -> Unit,
    selectedIcon: String, onIconChange: (String) -> Unit,
    colorOptions: List<String>,
    iconOptions: List<String>,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(stringResource(R.string.add_med_step_appearance))
        
        Text(text = stringResource(R.string.label_color), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colorOptions) { color ->
                ColorCircle(
                    colorHex = color,
                    selected = selectedColor == color,
                    onClick = { onColorChange(color) }
                )
            }
        }
        
        Text(text = stringResource(R.string.label_icon), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(iconOptions) { icon ->
                IconCircle(
                    iconName = icon,
                    colorHex = selectedColor,
                    selected = selectedIcon == icon,
                    onClick = { onIconChange(icon) }
                )
            }
        }
        
        // Preview
        MedReminderCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.jonataprezende.medreminder.ui.components.CommonComponents.MedicationColorIcon(
                    colorHex = selectedColor,
                    iconName = selectedIcon,
                    modifier = Modifier.size(48.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column {
                    Text("Nome do remédio", fontSize = 16.sp, fontWeight = FontWeight.W600)
                    Text("Dosagem • 08:00, 20:00", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.button_back), fontWeight = FontWeight.W600)
            }
            CTAButton(text = stringResource(R.string.button_save), onClick = onSave, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ScheduleTab(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = label, fontWeight = FontWeight.W600)
    }
}

@Composable
fun IntervalChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = androidx.compose.material3.ChipDefaults.filterChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun DurationTab(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = label, fontWeight = FontWeight.W600)
    }
}

@Composable
fun ColorCircle(colorHex: String, selected: Boolean, onClick: () -> Unit) {
    val color = try { android.graphics.Color.parseColor(colorHex) } catch (e: Exception) { MaterialTheme.colorScheme.primary.toArgb() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(color))
            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(if (selected) 0.dp else 2.dp)
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .then(androidx.compose.foundation.interaction.MutableInteractionSource())
            .then(androidx.compose.foundation.clickable(onClick))
    )
}

@Composable
fun IconCircle(iconName: String, colorHex: String, selected: Boolean, onClick: () -> Unit) {
    val color = try { android.graphics.Color.parseColor(colorHex) } catch (e: Exception) { MaterialTheme.colorScheme.primary.toArgb() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(color))
            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .padding(if (selected) 0.dp else 2.dp)
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .then(androidx.compose.foundation.interaction.MutableInteractionSource())
            .then(androidx.compose.foundation.clickable(onClick))
    ) {
        Icon(
            imageVector = getMedicationIcon(iconName),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp).align(Alignment.Center)
        )
    }
}

private fun getMedicationIcon(iconName: String): ImageVector {
    return when (iconName) {
        "capsule" -> Icons.Medical.Capsule
        "drops" -> Icons.Medical.Drops
        "syringe" -> Icons.Medical.Syringe
        "inhaler" -> Icons.Medical.Inhaler
        "patch" -> Icons.Medical.Patch
        else -> Icons.Medical.Pill
    }
}

private fun validateTimes(timesStr: String): Boolean {
    val times = timesStr.split(",").map { it.trim() }
    return times.all { time ->
        time.matches("\\d{2}:\\d{2}".toRegex()) && 
        time.split(":").let { (h, m) -> h.toIntOrNull()?.inRange(0, 23) == true && m.toIntOrNull()?.inRange(0, 59) == true }
    }
}

private fun showToast(context: android.content.Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

private suspend fun saveMedication(
    repository: MedRepository,
    context: android.content.Context,
    name: String, dosage: String, notes: String,
    scheduleType: ScheduleType, fixedTimes: String, intervalHours: Int,
    durationType: DurationType, durationDays: String,
    selectedColor: String, selectedIcon: String,
    onSaved: () -> Unit
) {
    val medication = Medication(
        name = name,
        dosage = dosage,
        notes = notes,
        colorHex = selectedColor,
        iconRes = selectedIcon
    )
    
    val medicationId = repository.insertMedication(medication)
    
    val startDate = System.currentTimeMillis()
    val endDate = if (durationType == DurationType.DAYS) {
        durationDays.toIntOrNull()?.let { days ->
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = startDate
            cal.add(java.util.Calendar.DAY_OF_MONTH, days)
            cal.timeInMillis
        } ?: 0
    } else 0
    
    val timesJson = if (scheduleType == ScheduleType.FIXED_TIME) {
        Json { ignoreUnknownKeys = true }.encodeToString(fixedTimes.split(",").map { it.trim() })
    } else "[]"
    
    val schedule = Schedule(
        medicationId = medicationId,
        type = scheduleType,
        timesJson = timesJson,
        intervalHours = if (scheduleType == ScheduleType.INTERVAL) intervalHours else 0,
        startDate = startDate,
        endDate = endDate
    )
    
    val scheduleId = repository.insertSchedule(schedule)
    
    // Gera dose logs e agenda alarmes
    val savedMed = repository.getMedicationById(medicationId)!!
    val savedSchedule = repository.getDoseLogsBySchedule(scheduleId).firstOrNull()?.let { 
        // Precisa buscar o schedule completo
        repository.getDoseLogsBySchedule(scheduleId) 
    }?.firstOrNull()?.let { _ -> 
        // Workaround: busca o schedule recém-criado
        null
    }
    // Melhor: usar o repository.generateDoseLogsForSchedule
    // Mas precisamos do objeto Schedule completo
    // Vamos buscar pela medicationId
    val schedules = repository.getActiveSchedulesByMedication(medicationId)
    for (s in schedules) {
        repository.generateDoseLogsForSchedule(s, savedMed)
    }
    
    // Agenda alarmes
    val alarmScheduler = com.jonataprezende.medreminder.alarm.AlarmScheduler.getInstance(
        context.applicationContext as com.jonataprezende.medreminder.MedReminderApplication
    )
    for (s in schedules) {
        alarmScheduler.rescheduleForSchedule(s)
    }
    
    onSaved()
}

enum class DurationType {
    CONTINUOUS,
    DAYS
}