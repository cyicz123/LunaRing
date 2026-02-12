package com.example.menstruation

import android.Manifest
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.notification.NotificationScheduler
import com.example.menstruation.notification.NotificationPermissionHelper
import com.example.menstruation.ui.navigation.NavGraph
import com.example.menstruation.ui.settings.SettingsViewModel
import com.example.menstruation.ui.theme.MenstruationTheme
import com.example.menstruation.ui.theme.PinkPrimary
import com.example.menstruation.ui.theme.PinkTransparent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var periodRepository: PeriodRepository
    @Inject lateinit var notificationScheduler: NotificationScheduler
    private lateinit var postNotificationsPermissionLauncher: ActivityResultLauncher<String>

    fun requestPostNotificationsPermissionFromUi() {
        postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postNotificationsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // If granted, schedule notifications immediately if user settings are enabled.
            if (granted) {
                MainScope().launch {
                    // Ensure UI reflects that notifications are enabled.
                    settingsRepository.updateNotificationEnabled(true)
                    val settings = settingsRepository.settings.first()
                    val periods = periodRepository.getAllPeriods().first()
                    val rt = settings.notificationSettings.reminderTime
                    notificationScheduler.scheduleAllNotifications(
                        periods = periods,
                        cycleLength = settings.cycleLength,
                        periodLength = settings.periodLength,
                        reminderTime = LocalTime.of(rt.hour, rt.minute),
                        enabled = settings.notificationSettings.enabled,
                        periodStartReminder = settings.notificationSettings.periodStartReminder,
                        periodEndReminder = settings.notificationSettings.periodEndReminder,
                        predictedPeriodReminder = settings.notificationSettings.predictedPeriodReminder
                    )
                }
            } else {
                // If denied, turn off notification enabled to avoid misleading "enabled but blocked".
                MainScope().launch {
                    settingsRepository.updateNotificationEnabled(false)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            var showNotifRationale by remember { mutableStateOf(false) }

            LaunchedEffect(
                uiState.isLoading,
                uiState.settings.notificationSettings.enabled,
                uiState.settings.notificationSettings.periodStartReminder,
                uiState.settings.notificationSettings.periodEndReminder,
                uiState.settings.notificationSettings.predictedPeriodReminder
            ) {
                val hasPerm = NotificationPermissionHelper.hasNotificationPermission(this@MainActivity)

                // Show a styled rationale sheet first; only then trigger the system permission dialog.
                val prefs = getSharedPreferences("yuehuan_prefs", MODE_PRIVATE)
                val alreadyRequested = prefs.getBoolean("notif_perm_requested", false)
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    uiState.isLoading.not() &&
                    uiState.settings.notificationSettings.enabled &&
                    !hasPerm &&
                    !alreadyRequested
                ) {
                    showNotifRationale = true
                }
            }

            val darkTheme = when (uiState.settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MenstruationTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                NavGraph(navController = navController)

                if (showNotifRationale) {
                    NotificationPermissionRationaleSheet(
                        onEnable = {
                            val prefs = getSharedPreferences("yuehuan_prefs", MODE_PRIVATE)
                            prefs.edit().putBoolean("notif_perm_requested", true).apply()
                            showNotifRationale = false
                            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onLater = {
                            val prefs = getSharedPreferences("yuehuan_prefs", MODE_PRIVATE)
                            prefs.edit().putBoolean("notif_perm_requested", true).apply()
                            showNotifRationale = false
                            MainScope().launch {
                                settingsRepository.updateNotificationEnabled(false)
                            }
                        },
                        onDismiss = {
                            // Treat dismiss as \"later\" to avoid repeated prompts on every recomposition.
                            val prefs = getSharedPreferences("yuehuan_prefs", MODE_PRIVATE)
                            prefs.edit().putBoolean("notif_perm_requested", true).apply()
                            showNotifRationale = false
                            MainScope().launch {
                                settingsRepository.updateNotificationEnabled(false)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationPermissionRationaleSheet(
    onEnable: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = PinkPrimary,
                modifier = androidx.compose.ui.Modifier.size(28.dp)
            )
            Text(
                text = "开启通知提醒",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "用于经期开始/结束提醒，以及预测经期提醒。你可以随时在设置里关闭。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PinkTransparent),
                shape = RoundedCornerShape(14.dp),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = androidx.compose.ui.Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "我们会在这些时刻通知你：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "• 经期开始 / 结束\\n• 预测经期到来前 1 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))

            Button(
                onClick = onEnable,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开启通知")
            }
            OutlinedButton(
                onClick = onLater,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("暂不")
            }
        }
    }
}