package com.example.adhdblockscheduler.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.adhdblockscheduler.ui.viewmodel.SchedulerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SchedulerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("설정") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("시간 및 블록 설정 (1시간 기준)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("시간 쪼개기 (알림 단위)") },
                supportingContent = { Text("집중 시간 동안 ${60 / uiState.blocksPerHour}분마다 짧은 알림을 줍니다.") },
                trailingContent = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${uiState.blocksPerHour}개")
                        Slider(
                            value = uiState.blocksPerHour.toFloat(),
                            onValueChange = { viewModel.updateBlocksPerHour(it.toInt()) },
                            valueRange = 1f..60f,
                            steps = 59,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            )

            ListItem(
                headlineContent = { Text("휴식 시간 설정") },
                supportingContent = { 
                    val focusMinutes = 60 - uiState.restMinutes
                    Text("집중 ${focusMinutes}분 후 ${uiState.restMinutes}분간 휴식합니다.") 
                },
                trailingContent = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${uiState.restMinutes}분")
                        Slider(
                            value = uiState.restMinutes.toFloat(),
                            onValueChange = { viewModel.updateRestMinutes(it.toInt()) },
                            valueRange = 0f..30f,
                            steps = 30,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            )

            ListItem(
                headlineContent = { Text("진동 알림") },
                supportingContent = { Text("알림 발생 시 진동을 켭니다.") },
                trailingContent = {
                    Switch(
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibration(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("알림 주기 설정") },
                supportingContent = { Text("집중 시간 중 ${uiState.alarmIntervalMinutes}분마다 알림을 줍니다.") },
                trailingContent = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${uiState.alarmIntervalMinutes}분")
                        Slider(
                            value = uiState.alarmIntervalMinutes.toFloat(),
                            onValueChange = { viewModel.updateAlarmIntervalMinutes(it.toInt()) },
                            valueRange = 5f..30f,
                            steps = 5,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "연동 설정", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("캘린더 자동 기록") },
                supportingContent = { Text("집중 블록 완료 시 삼성/구글 캘린더에 기록합니다.") },
                trailingContent = {
                    Switch(
                        checked = uiState.calendarSyncEnabled,
                        onCheckedChange = { viewModel.updateCalendarSync(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "버전 1.1.0",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
