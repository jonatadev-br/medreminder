package com.jonataprezende.medreminder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.alarm.AlarmScheduler
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.components.EmptyState
import com.jonataprezende.medreminder.ui.components.MedReminderCard
import com.jonataprezende.medreminder.ui.components.MedicationColorIcon
import com.jonataprezende.medreminder.ui.components.StatusChip
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: MedRepository,
    onAddMedication: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val alarmScheduler = remember { AlarmScheduler.getInstance(context) }

    val activeMeds by repository.getAllActiveMedications().collectAsState(initial = emptyList())
    var refreshKey by remember { mutableStateOf(0) }
    var todayDoses by remember { mutableStateOf<List<DoseWithMed>>(emptyList()) }

    LaunchedEffect(activeMeds, refreshKey) {
        todayDoses = loadTodayDoses(repository, activeMeds)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedication,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_med_title))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W600
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history_title))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (todayDoses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.Medication,
                        title = stringResource(R.string.home_empty_title),
                        subtitle = stringResource(R.string.home_empty_subtitle),
                        actionText = stringResource(R.string.home_empty_button),
                        onAction = onAddMedication
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(todayDoses, key = { it.doseLog.id }) { dose ->
                        DoseItem(
                            doseWithMed = dose,
                            onTake = {
                                scope.launch {
                                    markTaken(repository, alarmScheduler, dose)
                                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_taken))
                                    refreshKey++
                                }
                            },
                            onSnooze = {
                                scope.launch {
                                    snooze(repository, alarmScheduler, dose)
                                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_snoozed))
                                    refreshKey++
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoseItem(
    doseWithMed: DoseWithMed,
    onTake: () -> Unit,
    onSnooze: () -> Unit
) {
    val doseLog = doseWithMed.doseLog
    val medication = doseWithMed.medication
    val now = System.currentTimeMillis()
    val isOverdue = doseLog.scheduledAt < now && doseLog.status == DoseStatus.PENDING
    val isSnoozed = doseLog.snoozedUntil?.let { it > now } == true

    val statusColor = when {
        doseLog.status == DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
        isOverdue -> Color(0xFFE0453A)
        isSnoozed -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = when {
        doseLog.status == DoseStatus.TAKEN -> "Tomado"
        isOverdue -> "Atrasado"
        isSnoozed -> "Adiado"
        else -> "Pendente"
    }

    MedReminderCard(
        colors = CardDefaults.cardColors(
            containerColor = if (doseLog.status == DoseStatus.TAKEN)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MedicationColorIcon(
                    colorHex = medication.colorHex,
                    iconName = medication.iconRes
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = medication.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${medication.dosage} • ${DateTimeUtils.formatTime(doseLog.scheduledAt)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (medication.notes.isNotBlank()) {
                        Text(
                            text = medication.notes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.W500
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusChip(label = statusLabel, color = statusColor)

                if (doseLog.status == DoseStatus.PENDING) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onSnooze) {
                            Text("Adiar", fontSize = 13.sp, fontWeight = FontWeight.W600)
                        }
                        Button(
                            onClick = onTake,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("Tomar", fontSize = 13.sp, fontWeight = FontWeight.W600)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadTodayDoses(
    repository: MedRepository,
    medications: List<Medication>
): List<DoseWithMed> {
    val now = System.currentTimeMillis()
    val start = DateTimeUtils.startOfDay(now)
    val end = DateTimeUtils.endOfDay(now)
    val result = mutableListOf<DoseWithMed>()

    for (med in medications) {
        val schedules = repository.getActiveSchedulesByMedication(med.id)
        for (schedule in schedules) {
            val logs = repository.getDoseLogsBySchedule(schedule.id)
            for (log in logs) {
                if (log.scheduledAt in start..end) {
                    result.add(DoseWithMed(log, med, schedule))
                }
            }
        }
    }
    return result.sortedBy { it.doseLog.scheduledAt }
}

private suspend fun markTaken(
    repository: MedRepository,
    alarmScheduler: AlarmScheduler,
    doseWithMed: DoseWithMed
) {
    val updated = doseWithMed.doseLog.copy(
        takenAt = System.currentTimeMillis(),
        status = DoseStatus.TAKEN,
        snoozedUntil = null
    )
    repository.updateDoseLog(updated)
    alarmScheduler.cancelSingleDose(updated.id)
}

private suspend fun snooze(
    repository: MedRepository,
    alarmScheduler: AlarmScheduler,
    doseWithMed: DoseWithMed
) {
    val snoozeUntil = System.currentTimeMillis() + 10 * 60 * 1000
    val updated = doseWithMed.doseLog.copy(
        snoozedUntil = snoozeUntil,
        status = DoseStatus.PENDING
    )
    repository.updateDoseLog(updated)
    alarmScheduler.scheduleSnooze(updated, snoozeUntil)
}

data class DoseWithMed(
    val doseLog: DoseLog,
    val medication: Medication,
    val schedule: Schedule
)
