package com.jonataprezende.medreminder.ui.history

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.components.EmptyState
import com.jonataprezende.medreminder.ui.components.MedReminderCard
import com.jonataprezende.medreminder.ui.components.MedicationColorIcon
import com.jonataprezende.medreminder.ui.components.StatusChip
import com.jonataprezende.medreminder.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: MedRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val activeMeds by repository.getAllActiveMedications().collectAsState(initial = emptyList())
    var logs by remember { mutableStateOf<List<LogWithMed>>(emptyList()) }

    LaunchedEffect(activeMeds) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30L * 24 * 60 * 60 * 1000
        val allLogs = mutableListOf<LogWithMed>()
        for (med in activeMeds) {
            for (log in repository.getDoseLogsByMedicationAndDateRange(med.id, startTime, endTime)) {
                allLogs.add(LogWithMed(log, med))
            }
        }
        logs = allLogs.sortedByDescending { it.log.scheduledAt }
    }

    val filtered = remember(logs, filter) {
        logs.filter { item ->
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.TAKEN -> item.log.status == DoseStatus.TAKEN
                HistoryFilter.OVERDUE -> item.log.status == DoseStatus.OVERDUE
                HistoryFilter.SKIPPED -> item.log.status == DoseStatus.SKIPPED
            }
        }
    }

    val rows = remember(filtered) {
        val list = mutableListOf<HistoryRow>()
        var currentDate: String? = null
        for (item in filtered) {
            val date = DateTimeUtils.formatDate(item.log.scheduledAt)
            if (date != currentDate) {
                currentDate = date
                list.add(HistoryRow.Header(date))
            }
            list.add(HistoryRow.Entry(item))
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W600
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.button_back))
                    }
                },
                actions = {
                    IconButton(onClick = { shareHistory(context, filtered) }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.button_export))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.values().forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = stringResource(R.string.history_empty_title),
                        subtitle = stringResource(R.string.history_empty_subtitle)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rows) { row ->
                        when (row) {
                            is HistoryRow.Header -> DateHeader(row.date)
                            is HistoryRow.Entry -> HistoryItem(logWithMed = row.item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Text(
        text = date,
        fontSize = 13.sp,
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
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
                        fontWeight = FontWeight.W600
                    )
                    Text(
                        text = buildString {
                            append(medication.dosage)
                            append(" • ")
                            append(DateTimeUtils.formatTime(log.scheduledAt))
                            if (takenTime != null) append(" • Tomado: $takenTime")
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusChip(label = statusLabel, color = statusColor)
        }
    }
}

private fun shareHistory(context: Context, logs: List<LogWithMed>) {
    val builder = StringBuilder()
    builder.appendLine(context.getString(R.string.export_title))
    builder.appendLine(context.getString(R.string.export_generated, DateTimeUtils.formatDateTime(System.currentTimeMillis())))
    builder.appendLine()

    var currentDate: String? = null
    for (item in logs) {
        val date = DateTimeUtils.formatDate(item.log.scheduledAt)
        if (date != currentDate) {
            currentDate = date
            builder.appendLine("== $date ==")
        }
        val takenStr = item.log.takenAt?.let { " (${DateTimeUtils.formatTime(it)})" } ?: ""
        builder.appendLine(
            "${statusText(item.log.status)} ${item.medication.name} - " +
                "${item.medication.dosage} - ${DateTimeUtils.formatTime(item.log.scheduledAt)}$takenStr"
        )
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_title))
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_title)))
}

private fun statusText(status: DoseStatus): String = when (status) {
    DoseStatus.TAKEN -> "[TOMADO]"
    DoseStatus.OVERDUE -> "[ATRASADO]"
    DoseStatus.SKIPPED -> "[PULADO]"
    else -> "[PENDENTE]"
}

data class LogWithMed(
    val log: DoseLog,
    val medication: Medication
)

sealed class HistoryRow {
    data class Header(val date: String) : HistoryRow()
    data class Entry(val item: LogWithMed) : HistoryRow()
}

enum class HistoryFilter(val label: String) {
    ALL("Todos"),
    TAKEN("Tomados"),
    OVERDUE("Atrasados"),
    SKIPPED("Pulados")
}
