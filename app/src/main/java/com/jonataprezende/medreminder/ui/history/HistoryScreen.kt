package com.jonataprezende.medreminder.ui.history

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
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.components.CommonComponents.EmptyState
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedReminderCard
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedicationColorIcon
import com.jonataprezende.medreminder.ui.components.CommonComponents.StatusChip
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    repository: MedRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    
    val activeMeds by repository.getAllActiveMedications().observeAsState(initialValue = emptyList())
    
    // Busca logs dos últimos 30 dias
    val logs by remember {
        mutableStateOf<List<LogWithMed>>(emptyList())
    }
    
    androidx.compose.runtime.LaunchedEffect(activeMeds, filter) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30L * 24 * 60 * 60 * 1000 // 30 dias
        
        val allLogs = mutableListOf<LogWithMed>()
        for (med in activeMeds) {
            val medLogs = repository.getDoseLogsByMedicationAndDateRange(med.id!!, startTime, endTime)
            for (log in medLogs) {
                allLogs.add(LogWithMed(log, med))
            }
        }
        // Ordena por data decrescente (mais recente primeiro)
        logs = allLogs.sortedByDescending { it.log.scheduledAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.history_title), fontSize = 20.sp, fontWeight = FontWeight.W600) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.button_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    // Export button
                    IconButton(onClick = { exportHistory(logs, context) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.button_export))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.values().forEach { f ->
                    androidx.compose.material3.FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) },
                        colors = androidx.compose.material3.ChipDefaults.filterChipColors(
                            containerColor = if (filter == f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (filter == f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            
            // Filtered logs
            val filteredLogs = logs.filter { log ->
                when (filter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.TAKEN -> log.log.status == DoseStatus.TAKEN
                    HistoryFilter.OVERDUE -> log.log.status == DoseStatus.OVERDUE
                    HistoryFilter.SKIPPED -> log.log.status == DoseStatus.SKIPPED
                }
            }
            
            if (filteredLogs.isEmpty()) {
                EmptyState(
                    icon = Icons.Medical.History,
                    title = stringResource(R.string.history_empty_title),
                    subtitle = stringResource(R.string.history_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var currentDate: String? = null
                    items(filteredLogs) { logWithMed ->
                        val logDate = DateTimeUtils.formatDate(logWithMed.log.scheduledAt)
                        if (logDate != currentDate) {
                            currentDate = logDate
                            stickyHeader {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    Text(
                                        text = logDate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.W600,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                        HistoryItem(logWithMed = logWithMed)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(logWithMed: LogWithMed) {
    val log = logWithMed.log
    val medication = logWithMed.medication
    
    val statusColor = when (log.status) {
        DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
        DoseStatus.OVERDUE -> Color(0xFFE0453A)
        DoseStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    val statusLabel = when (log.status) {
        DoseStatus.TAKEN -> "Tomado"
        DoseStatus.OVERDUE -> "Atrasado"
        DoseStatus.SKIPPED -> "Pulado"
        else -> "Pendente"
    }
    
    val takenTime = log.takenAt?.let { DateTimeUtils.formatTime(it) }
    
    MedReminderCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        fontWeight = FontWeight.W600
                    )
                    Text(
                        text = "${medication.dosage} • Agendado: ${DateTimeUtils.formatTime(log.scheduledAt)}${takenTime?.let { " • Tomado: $it" }}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            StatusChip(label = statusLabel, color = statusColor)
        }
    }
}

private fun exportHistory(logs: List<LogWithMed>, context: android.content.Context) {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        val builder = StringBuilder()
        builder.appendLine("MedReminder - Resumo do Tratamento")
        builder.appendLine("")
        builder.appendLine("Gerado em: ${DateTimeUtils.formatDateTime(System.currentTimeMillis())}")
        builder.appendLine("")
        
        var currentDate: String? = null
        for (logWithMed in logs) {
            val log = logWithMed.log
            val med = logWithMed.medication
            val logDate = DateTimeUtils.formatDate(log.scheduledAt)
            
            if (logDate != currentDate) {
                currentDate = logDate
                builder.appendLine("=== $logDate ===")
            }
            
            val statusIcon = when (log.status) {
                DoseStatus.TAKEN -> "✅"
                DoseStatus.OVERDUE -> "❌"
                DoseStatus.SKIPPED -> "⏭️"
                else -> "⏳"
            }
            
            val takenStr = log.takenAt?.let { " (${DateTimeUtils.formatTime(it)})" } ?: ""
            builder.appendLine("$statusIcon ${med.name} - ${med.dosage} - ${DateTimeUtils.formatTime(log.scheduledAt)}$takenStr")
        }
        
        val text = builder.toString()
        
        // Share via Intent
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.export_title))
        intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.export_title)))
    }
}

data class LogWithMed(
    val log: DoseLog,
    val medication: Medication
)

enum class HistoryFilter(val label: String) {
    ALL("Todos"),
    TAKEN("Tomados"),
    OVERDUE("Atrasados"),
    SKIPPED("Pulados")
}