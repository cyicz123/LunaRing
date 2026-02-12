package com.example.menstruation.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menstruation.ui.home.components.CalendarView
import com.example.menstruation.ui.home.components.RecordBottomSheet
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生理期记录") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, null) },
                    label = { Text("日历") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("统计") },
                    selected = false,
                    onClick = onNavigateToStats
                )
            }
        }
    ) { padding ->
        CalendarView(
            records = uiState.records,
            periods = uiState.periods,
            predictedPeriod = uiState.predictedPeriod,
            settings = uiState.settings,
            currentDate = LocalDate.now(),
            onDateClick = { selectedDate = it },
            onMonthChange = { },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )

        // 记录面板
        selectedDate?.let { date ->
            val isInPeriod = viewModel.isInPeriod(date)
            RecordBottomSheet(
                date = date,
                record = uiState.records[date],
                isInPeriod = isInPeriod,
                onDismiss = { selectedDate = null },
                onSave = { viewModel.saveRecord(it) },
                onStartPeriod = {
                    viewModel.startPeriod(date)
                },
                onEndPeriod = {
                    viewModel.endPeriod(date)
                }
            )
        }
    }
}
