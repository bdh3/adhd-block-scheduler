package com.example.adhdblockscheduler.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.adhdblockscheduler.BuildConfig
import com.example.adhdblockscheduler.ui.viewmodel.SchedulerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SchedulerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    var alarmInterval by remember { mutableIntStateOf(uiState.alarmIntervalMinutes) }
    var restMinutes by remember { mutableIntStateOf(uiState.restMinutes) }
    var vibrationEnabled by remember { mutableStateOf(uiState.vibrationEnabled) }

    // 초기값 동기화 (한 번만)
    LaunchedEffect(uiState.alarmIntervalMinutes) {
        alarmInterval = uiState.alarmIntervalMinutes
    }
    LaunchedEffect(uiState.restMinutes) {
        restMinutes = uiState.restMinutes
    }
    LaunchedEffect(uiState.vibrationEnabled) {
        vibrationEnabled = uiState.vibrationEnabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                actions = {
                    val isModified = (uiState.alarmIntervalMinutes != alarmInterval) || 
                                     (uiState.restMinutes != restMinutes) ||
                                     (uiState.vibrationEnabled != vibrationEnabled)

                    Button(
                        onClick = {
                            viewModel.saveSettings(alarmInterval, restMinutes, vibrationEnabled, false)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = isModified
                    ) {
                        Text(if (isModified) "저장" else "저장됨")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 단일 작업 기본 전략",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "계획 없이 바로 타이머를 시작할 때 적용되는 기본값입니다. (개별 작업 생성 시 변경 가능)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // 전략 프리셋 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = restMinutes == 0,
                            onClick = { alarmInterval = 15; restMinutes = 0 },
                            label = { Text("연속 집중") }
                        )
                        FilterChip(
                            selected = alarmInterval == 25 && restMinutes == 5,
                            onClick = { alarmInterval = 25; restMinutes = 5 },
                            label = { Text("25/5 (뽀모도로)") }
                        )
                        FilterChip(
                            selected = alarmInterval == 50 && restMinutes == 10,
                            onClick = { alarmInterval = 50; restMinutes = 10 },
                            label = { Text("50/10 (고집중)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("집중: ${alarmInterval}분", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = when(alarmInterval) { 15 -> 0f; 25 -> 1f; 30 -> 2f; 45 -> 3f; 50 -> 4f; 60 -> 5f; else -> 0f },
                            onValueChange = { 
                                alarmInterval = when(it.toInt()) { 0 -> 15; 1 -> 25; 2 -> 30; 3 -> 45; 4 -> 50; 5 -> 60; else -> 15 }
                            },
                            valueRange = 0f..5f,
                            steps = 4,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("휴식: ${restMinutes}분", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = when(restMinutes) { 0 -> 0f; 5 -> 1f; 10 -> 2f; 15 -> 3f; else -> 0f },
                            onValueChange = { 
                                restMinutes = when(it.toInt()) { 0 -> 0; 1 -> 5; 2 -> 10; 3 -> 15; else -> 0 }
                            },
                            valueRange = 0f..3f,
                            steps = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("알림 및 시스템", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("진동 알림") },
                supportingContent = { Text("구간 전환 시 진동을 켭니다.") },
                trailingContent = {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("시스템 설정", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            val isIgnoringBattery = viewModel.isIgnoringBatteryOptimizations()
            
            ListItem(
                headlineContent = { Text("배터리 최적화 제외") },
                supportingContent = { 
                    Text(if (isIgnoringBattery) 
                        "백그라운드에서 정확한 알람을 위해 배터리 제한이 해제된 상태입니다." 
                        else "화면이 꺼졌을 때 알람이 누락되는 것을 방지하기 위해 이 설정이 권장됩니다.") 
                },
                trailingContent = {
                    Button(
                        onClick = { viewModel.requestIgnoreBatteryOptimizations() },
                        enabled = !isIgnoringBattery,
                        colors = if (isIgnoringBattery) 
                            ButtonDefaults.filledTonalButtonColors() 
                            else ButtonDefaults.buttonColors()
                    ) {
                        Text(if (isIgnoringBattery) "설정됨" else "설정하기")
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "버전 ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
