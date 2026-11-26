package com.jonataprezende.medreminder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Default as IconsDefault
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.components.CommonComponents.CTAButton
import com.jonataprezende.medreminder.ui.components.CommonComponents.EmptyState
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedReminderCard
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedicationColorIcon
import com.jonataprezende.medreminder.ui.components.CommonComponents.StatusChip
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun HomeScreen(
    repository: MedRepository,
    onAddMedication: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeMeds by repository.getAllActiveMedications().observeAsState(initialValue = emptyList())
    
    // Busca doses de hoje para todos os medicamentos ativos
    val todayDoses by remember {
        mutableStateOf<List<DoseWithMed>>(emptyList())
    }

    // Atualiza doses de hoje quando medicamentos mudam
    androidx.compose.runtime.LaunchedEffect(activeMeds) {
        val now = System.currentTimeMillis()
        val startOfDay = DateTimeUtils.startOfDay(now)
        val endOfDay = DateTimeUtils.endOfDay(now)
        
        val allDoses = mutableListOf<DoseWithMed>()
        for (med in activeMeds) {
            val schedules = repository.getActiveSchedulesByMedication(med.id!!)
            for (schedule in schedules) {
                val logs = repository.getDoseLogsBySchedule(schedule.id!!)
                    .filter { it.scheduledAt >= startOfDay && it.scheduledAt <= endOfDay }
                for (log in logs) {
                    allDoses.add(DoseWithMed(log, med, schedule))
                }
            }
        }
        // Ordena por horário
        todayDoses = allDoses.sortedBy { it.doseLog.scheduledAt }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedication,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(IconsDefault.Add, contentDescription = stringResource(R.string.add_med_title))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top App Bar
                TopAppBar(
                    title = { Text(text = stringResource(R.string.home_title), fontSize = 20.sp, fontWeight = FontWeight.W600) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    actions = {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(History, contentDescription = stringResource(R.string.history_title))
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                )

                // Content
                if (todayDoses.isEmpty()) {
                    EmptyState(
                        icon = IconsMedical.Pill,
                        title = stringResource(R.string.home_empty_title),
                        subtitle = stringResource(R.string.home_empty_subtitle),
                        actionText = stringResource(R.string.home_empty_button),
                        onAction = onAddMedication
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 100.dp), // Space for FAB
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todayDoses) { doseWithMed ->
                            DoseItem(
                                doseWithMed = doseWithMed,
                                onTake = { scope.launch { markAsTaken(doseWithMed, snackbarHostState) } },
                                onSnooze = { scope.launch { snoozeDose(doseWithMed, snackbarHostState) } }
                            )
                        }
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
    val isOverdue = doseLog.scheduledAt < System.currentTimeMillis() && doseLog.status == DoseStatus.PENDING
    val isSnoozed = doseLog.snoozedUntil != null && doseLog.snoozedUntil!! > System.currentTimeMillis()

    val statusColor = when {
        doseLog.status == DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
        isOverdue -> Color(0xFFE0453A) // Red
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
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (doseLog.status == DoseStatus.TAKEN) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedicationColorIcon(
                    colorHex = medication.colorHex,
                    iconName = medication.iconRes,
                    modifier = Modifier.size(48.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.Center) {
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

            // Right: Status + Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusChip(label = statusLabel, color = statusColor)
                
                if (doseLog.status == DoseStatus.PENDING) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = onSnooze,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(
                                text = "Adiar 10 min",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600
                            )
                        }
                        
                        androidx.compose.material3.FilledButton(
                            onClick = onTake,
                            colors = androidx.compose.material3.ButtonDefaults.filledButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = "Tomar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun markAsTaken(
    doseWithMed: DoseWithMed,
    snackbarHostState: SnackbarHostState
) {
    val repository = MedRepository.getInstance(android.app.Application() as com.jonataprezende.medreminder.MedReminderApplication)
    val doseLog = doseWithMed.doseLog.copy(
        takenAt = System.currentTimeMillis(),
        status = com.jonataprezende.medreminder.data.entity.DoseStatus.TAKEN,
        snoozedUntil = null
    )
    repository.updateDoseLog(doseLog)
    
    // Cancela alarme se houver
    val alarmScheduler = com.jonataprezende.medreminder.alarm.AlarmScheduler.getInstance(
        android.app.Application() as com.jonataprezende.medreminder.MedReminderApplication
    )
    alarmScheduler.cancelSingleDose(doseLog.id!!)
    
    snackbarHostState.showSnackbar("Marcado como tomado")
}

private suspend fun snoozeDose(
    doseWithMed: DoseWithMed,
    snackbarHostState: SnackbarHostState
) {
    val repository = MedRepository.getInstance(android.app.Application() as com.jonataprezende.medreminder.MedReminderApplication)
    val snoozeUntil = System.currentTimeMillis() + 10 * 60 * 1000
    val doseLog = doseWithMed.doseLog.copy(
        snoozedUntil = snoozeUntil,
        status = com.jonataprezende.medreminder.data.entity.DoseStatus.PENDING
    )
    repository.updateDoseLog(doseLog)
    
    // Reagenda alarme
    val alarmScheduler = com.jonataprezende.medreminder.alarm.AlarmScheduler.getInstance(
        android.app.Application() as com.jonataprezende.medreminder.MedReminderApplication
    )
    alarmScheduler.scheduleSnooze(doseLog, snoozeUntil)
    
    snackbarHostState.showSnackbar("Adiado para 10 minutos")
}

data class DoseWithMed(
    val doseLog: DoseLog,
    val medication: Medication,
    val schedule: com.jonataprezende.medreminder.data.entity.Schedule
)