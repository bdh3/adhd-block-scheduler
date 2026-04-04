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
            
            val blockDuration = 60 / uiState.blocksPerHour
            ListItem(
                headlineContent = { Text("시간 쪼개기 (블록 수)") },
                supportingContent = { Text("1시간을 ${uiState.blocksPerHour}개로 나눕니다. (개당 ${blockDuration}분)") },
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
                headlineContent = { Text("집중 블록 개수") },
                supportingContent = { 
                    val focusMinutes = blockDuration * uiState.focusBlocksCount
                    val restMinutes = 60 - (blockDuration * uiState.focusBlocksCount)
                    Text("집중 ${uiState.focusBlocksCount}개(${focusMinutes}분), 휴식 ${uiState.blocksPerHour - uiState.focusBlocksCount}개(${restMinutes}분)") 
                },
                trailingContent = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${uiState.focusBlocksCount}개")
                        Slider(
                            value = uiState.focusBlocksCount.toFloat(),
                            onValueChange = { 
                                if (it.toInt() < uiState.blocksPerHour) {
                                    viewModel.updateFocusBlocksCount(it.toInt())
                                }
                            },
                            valueRange = 0f..maxOf(1f, (uiState.blocksPerHour - 1).toFloat()),
                            steps = if (uiState.blocksPerHour > 1) uiState.blocksPerHour - 1 else 0,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            )

            ListItem(
                headlineContent = { Text("중간 알림 간격") },
                supportingContent = { 
                    Text(if (uiState.notificationInterval > 0) "${uiState.notificationInterval}분마다 짧은 진동을 줍니다." else "중간 알림을 끕니다.") 
                },
                trailingContent = {
                    Slider(
                        value = uiState.notificationInterval.toFloat(),
                        onValueChange = { viewModel.updateNotificationInterval(it) },
                        valueRange = 0f..15f,
                        steps = 15, // 1분 단위
                        modifier = Modifier.width(120.dp)
                    )
                }
            )

            ListItem(
                headlineContent = { Text("진동 알림") },
                supportingContent = { Text("블록 완료 시 진동으로 알려줍니다.") },
                trailingContent = {
                    Switch(
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibration(it) }
                    )
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
                text = "버전 1.0.2",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
