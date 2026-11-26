package com.jonataprezende.medreminder.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.ui.components.CommonComponents.CTAButton
import com.jonataprezende.medreminder.ui.components.CommonComponents.MedReminderCard
import com.jonataprezende.medreminder.ui.components.CommonComponents.SectionHeader
import com.jonataprezende.medreminder.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var darkTheme by remember { mutableStateOf(false) }
    
    val notificationPerm by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    val exactAlarmPerm by remember { mutableStateOf(PermissionUtils.hasExactAlarmPermission(context)) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        darkTheme = (context.getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager).nightMode == android.app.UiModeManager.MODE_NIGHT_YES
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title), fontSize = 20.sp, fontWeight = FontWeight.W600) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.button_back))
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
            // Theme section
            SectionHeader(stringResource(R.string.settings_theme))
            MedReminderCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tema do aplicativo", fontSize = 16.sp, fontWeight = FontWeight.W500)
                        Text("Escolha entre claro, escuro ou sistema", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.SegmentedButton(
                        selected = { darkTheme },
                        onClick = { 
                            // Toggle theme - em produção usaria AppCompatDelegate
                            darkTheme = !darkTheme
                            // Aplica tema
                            val delegate = androidx.appcompat.app.AppCompatDelegate
                            delegate.setDefaultNightMode(
                                if (darkTheme) delegate.MODE_NIGHT_YES else delegate.MODE_NIGHT_NO
                            )
                        },
                        single = true
                    ) {
                        androidx.compose.material3.SegmentedButtonItem(
                            label = { Text(stringResource(R.string.theme_light)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        androidx.compose.material3.SegmentedButtonItem(
                            label = { Text(stringResource(R.string.theme_dark)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        androidx.compose.material3.SegmentedButtonItem(
                            label = { Text(stringResource(R.string.theme_system)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Permissions section
            SectionHeader(stringResource(R.string.settings_permissions))
            MedReminderCard {
                PermissionRow(
                    title = stringResource(R.string.perm_notifications),
                    subtitle = if (notificationPerm) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied),
                    granted = notificationPerm,
                    onRequest = { 
                        if (!notificationPerm) {
                            PermissionUtils.requestNotificationPermission(context as android.app.Activity)
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                PermissionRow(
                    title = stringResource(R.string.perm_exact_alarm),
                    subtitle = if (exactAlarmPerm) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied),
                    granted = exactAlarmPerm,
                    onRequest = { 
                        if (!exactAlarmPerm) {
                            PermissionUtils.requestExactAlarmPermission(context as android.app.Activity)
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )
            }
            
            // Test notification
            SectionHeader("Teste")
            MedReminderCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Testar notificação", fontSize = 16.sp, fontWeight = FontWeight.W500)
                        Text("Envia uma notificação de teste para verificar som e vibração", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CTAButton(
                        text = stringResource(R.string.button_test_notification),
                        onClick = { 
                            com.jonataprezende.medreminder.alarm.NotificationHelper.showTestNotification(context)
                        },
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
            
            // About
            SectionHeader(stringResource(R.string.settings_about))
            MedReminderCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Versão", fontSize = 16.sp)
                        Text(stringResource(R.string.about_version), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_description),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.W500)
            Text(subtitle, fontSize = 13.sp, color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Text(if (granted) "Configurações" else "Conceder", fontSize = 13.sp, fontWeight = FontWeight.W600)
        }
    }
}