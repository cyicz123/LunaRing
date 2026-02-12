package com.example.menstruation.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val context = LocalContext.current

    // 监听事件
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    SettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onNavigateToHome = onNavigateToHome,
        onNavigateToStats = onNavigateToStats,
        onExportClick = {
            val fileName = viewModel.generateExportFileName()
            exportLauncher.launch(fileName)
        },
        onImportClick = {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onResetDataClick = {
            viewModel.resetRecordsData()
        }
    )
}
