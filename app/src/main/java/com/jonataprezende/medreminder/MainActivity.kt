package com.jonataprezende.medreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.jonataprezende.medreminder.alarm.NotificationHelper
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.addmed.AddMedicationScreen
import com.jonataprezende.medreminder.ui.history.HistoryScreen
import com.jonataprezende.medreminder.ui.home.HomeScreen
import com.jonataprezende.medreminder.ui.settings.SettingsScreen
import com.jonataprezende.medreminder.util.PermissionUtils

class MainActivity : ComponentActivity() {
    private val repository: MedRepository by lazy { MedRepository.getInstance(this) }
    private val alarmScheduler by lazy { com.jonataprezende.medreminder.alarm.AlarmScheduler.getInstance(this) }

    // Permission request launchers
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled in UI
    }

    private val requestExactAlarmPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Result handled in UI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cria canal de notificação
        NotificationHelper.createNotificationChannel(this)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(repository = repository, alarmScheduler = alarmScheduler)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verifica permissões críticas no Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                // UI mostrará botão para conceder
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // UI mostrará botão para conceder
            }
        }
    }
}

@Composable
fun AppNavHost(
    repository: MedRepository,
    alarmScheduler: com.jonataprezende.medreminder.alarm.AlarmScheduler
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            repository = repository,
            onAddMedication = { currentScreen = Screen.ADD_MEDICATION },
            onNavigateToHistory = { currentScreen = Screen.HISTORY },
            onNavigateToSettings = { currentScreen = Screen.SETTINGS }
        )
        Screen.ADD_MEDICATION -> AddMedicationScreen(
            repository = repository,
            onBack = { currentScreen = Screen.HOME },
            onSaved = { currentScreen = Screen.HOME }
        )
        Screen.HISTORY -> HistoryScreen(
            repository = repository,
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.SETTINGS -> SettingsScreen(
            onBack = { currentScreen = Screen.HOME }
        )
    }
}

enum class Screen {
    HOME,
    ADD_MEDICATION,
    HISTORY,
    SETTINGS
}