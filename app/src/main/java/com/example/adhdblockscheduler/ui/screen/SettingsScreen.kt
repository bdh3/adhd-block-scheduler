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
    var calendarSyncEnabled by remember { mutableStateOf(false) }
    var blockDuration by remember { mutableStateOf(15f) }

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
            Text("일반 설정", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("블록 길이 (분)") },
                supportingContent = { Text("${blockDuration.toInt()}분 단위로 집중 시간을 나눕니다.") },
                trailingContent = {
                    Slider(
                        value = blockDuration,
                        onValueChange = { blockDuration = it },
                        valueRange = 10f..30f,
                        steps = 3,
                        modifier = Modifier.width(100.dp)
                    )
                }
            )

            Divider()

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
                        checked = calendarSyncEnabled,
                        onCheckedChange = { calendarSyncEnabled = it }
                    )
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "버전 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
