package com.jonataprezende.medreminder

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jonataprezende.medreminder.alarm.NotificationHelper
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.ui.addmed.AddMedicationScreen
import com.jonataprezende.medreminder.ui.history.HistoryScreen
import com.jonataprezende.medreminder.ui.home.HomeScreen
import com.jonataprezende.medreminder.ui.settings.SettingsScreen
import com.jonataprezende.medreminder.ui.theme.Theme
import com.jonataprezende.medreminder.util.PermissionUtils

class MainActivity : ComponentActivity() {
    private val repository: MedRepository by lazy {
        MedRepository.getInstance(applicationContext)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* resultado tratado na tela */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.hasNotificationPermission(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences("medreminder_prefs", Context.MODE_PRIVATE)
        val themeMode = prefs.getString("theme_mode", "system") ?: "system"

        setContent {
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            Theme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(repository = repository)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(repository: MedRepository) {
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
