package com.example.adhdblockscheduler.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adhdblockscheduler.BuildConfig
import com.example.adhdblockscheduler.ui.viewmodel.SchedulerViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SchedulerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    var alarmInterval by remember { mutableIntStateOf(uiState.alarmIntervalMinutes) }
    var restMinutes by remember { mutableIntStateOf(uiState.restMinutes) }
    var vibrationEnabled by remember { mutableStateOf(uiState.vibrationEnabled) }

    val divisorsOf60 = listOf(1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60)

    fun getValidRestFor60(focus: Int, currentRest: Int): Int {
        val possibleSums = divisorsOf60.filter { it > focus }
        if (possibleSums.isEmpty()) return 60 - focus
        return possibleSums.minByOrNull { abs((it - focus) - currentRest) }?.let { it - focus } ?: (60 - focus)
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

                    // 2. 모드 선택 (탭 스타일)
                    var isCycleMode by remember { mutableStateOf(restMinutes > 0) }
                    
                    Column {
                        Text("집중 방식", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (!isCycleMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        isCycleMode = false
                                        alarmInterval = 15
                                        restMinutes = 0
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "연속 집중", 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (!isCycleMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (isCycleMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        isCycleMode = true
                                        if (restMinutes == 0) {
                                            alarmInterval = 25
                                            restMinutes = 5
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "인터벌 사이클", 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isCycleMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. 상세 설정 영역
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isCycleMode) {
                                // 연속 집중 모드
                                Text("알람 주기 (일정 시간마다 리마인드)", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(5, 10, 15, 30).forEach { interval ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(MaterialTheme.shapes.small)
                                                .background(if (alarmInterval == interval) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                .clickable { alarmInterval = interval },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${interval}분",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (alarmInterval == interval) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 인터벌 사이클 모드
                                Text("빠른 프리셋", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    AssistChip(
                                        onClick = { 
                                            alarmInterval = 25
                                            restMinutes = 5
                                        },
                                        label = { Text("25/5", fontSize = 12.sp) },
                                        leadingIcon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                    AssistChip(
                                        onClick = { 
                                            alarmInterval = 50
                                            restMinutes = 10
                                        },
                                        label = { Text("50/10", fontSize = 12.sp) },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                
                                Text("상세 값 수정", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("집중: ${alarmInterval}분", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                                        Slider(
                                            value = alarmInterval.toFloat(),
                                            onValueChange = { 
                                                alarmInterval = (it.toInt() / 5) * 5
                                                restMinutes = getValidRestFor60(alarmInterval, restMinutes)
                                            },
                                            valueRange = 5f..55f,
                                            modifier = Modifier.weight(1f),
                                            steps = 9 // 5, 10, 15, ..., 55
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("휴식: ${restMinutes}분", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                                        Slider(
                                            value = restMinutes.toFloat(),
                                            onValueChange = { 
                                                val requestedRest = it.toInt().coerceAtLeast(1)
                                                val targetSum = divisorsOf60.filter { d -> d > alarmInterval }
                                                    .minByOrNull { d -> abs((d - alarmInterval) - requestedRest) } ?: 60
                                                restMinutes = targetSum - alarmInterval
                                            },
                                            valueRange = 1f..(60 - alarmInterval).toFloat(),
                                            modifier = Modifier.weight(1f),
                                            steps = (60 - alarmInterval - 1).coerceAtLeast(0)
                                        )
                                    }
                                    Text(
                                        "* 60분의 약수에 맞춰 자동 보정됩니다.", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
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
