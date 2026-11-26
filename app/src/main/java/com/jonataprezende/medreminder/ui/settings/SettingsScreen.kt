package com.jonataprezende.medreminder.ui.settings

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.alarm.NotificationHelper
import com.jonataprezende.medreminder.ui.components.CTAButton
import com.jonataprezende.medreminder.ui.components.MedReminderCard
import com.jonataprezende.medreminder.ui.components.SectionHeader
import com.jonataprezende.medreminder.util.PermissionUtils

private const val PREFS_NAME = "medreminder_prefs"
private const val KEY_THEME = "theme_mode"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString(KEY_THEME, "system") ?: "system") }
    var notificationGranted by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var exactAlarmGranted by remember { mutableStateOf(PermissionUtils.hasExactAlarmPermission(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W600
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.button_back))
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
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_theme))
            MedReminderCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Tema do aplicativo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500
                    )
                    Text(
                        text = "Claro, escuro ou seguir o sistema",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip(stringResource(R.string.theme_light), themeMode == "light") {
                            themeMode = "light"
                            applyTheme(context, prefs, "light")
                        }
                        ThemeChip(stringResource(R.string.theme_dark), themeMode == "dark") {
                            themeMode = "dark"
                            applyTheme(context, prefs, "dark")
                        }
                        ThemeChip(stringResource(R.string.theme_system), themeMode == "system") {
                            themeMode = "system"
                            applyTheme(context, prefs, "system")
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_permissions))
            MedReminderCard {
                PermissionRow(
                    title = stringResource(R.string.perm_notifications),
                    granted = notificationGranted,
                    onRequest = {
                        val activity = context as? Activity
                        if (!notificationGranted && activity != null) {
                            PermissionUtils.requestNotificationPermission(activity)
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                PermissionRow(
                    title = stringResource(R.string.perm_exact_alarm),
                    granted = exactAlarmGranted,
                    onRequest = {
                        val activity = context as? Activity
                        if (!exactAlarmGranted && activity != null) {
                            PermissionUtils.requestExactAlarmPermission(activity)
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )
            }

            SectionHeader("Teste")
            MedReminderCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Testar notificação", fontSize = 16.sp, fontWeight = FontWeight.W500)
                        Text(
                            text = "Envia uma notificação de teste para verificar som e vibração",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { NotificationHelper.showTestNotification(context) },
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Testar", fontSize = 13.sp, fontWeight = FontWeight.W600)
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_about))
            MedReminderCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Versão", fontSize = 16.sp)
                        Text(
                            text = stringResource(R.string.about_version),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_description),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun PermissionRow(
    title: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.W500)
            Text(
                text = if (granted) "Concedida" else "Negada",
                fontSize = 13.sp,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = if (granted) "Configurações" else "Conceder",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600
            )
        }
    }
}

private fun applyTheme(context: Context, prefs: SharedPreferences, mode: String) {
    prefs.edit().putString(KEY_THEME, mode).apply()
    (context as? Activity)?.recreate()
}
