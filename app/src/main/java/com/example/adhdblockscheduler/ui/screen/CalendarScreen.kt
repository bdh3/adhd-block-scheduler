package com.example.adhdblockscheduler.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adhdblockscheduler.model.ScheduleBlock
import com.example.adhdblockscheduler.ui.viewmodel.SchedulerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: SchedulerViewModel,
    onNavigateToTimer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("포커스 플로우 캘린더") },
                actions = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        viewModel.selectDate(cal.timeInMillis)
                    }) {
                        Text("<")
                    }
                    Text(
                        text = if (isToday(selectedDate)) "오늘" 
                               else if (isTomorrow(selectedDate)) "내일"
                               else formatDate(selectedDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        viewModel.selectDate(cal.timeInMillis)
                    }) {
                        Text(">")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 시간대별 그리드
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items((0..23).toList()) { hour ->
                    val schedule = uiState.dailySchedules.find { 
                        val cal = Calendar.getInstance().apply { timeInMillis = it.startTimeMillis }
                        cal.get(Calendar.HOUR_OF_DAY) == hour 
                    }

                    TimeRow(
                        hour = hour,
                        schedule = schedule,
                        isCurrent = schedule?.id != null && schedule.id == uiState.currentScheduleId,
                        isRunning = uiState.isRunning,
                        onClick = {
                            if (schedule == null) {
                                selectedHour = hour
                                showAddTaskDialog = true
                            } else {
                                // 기존 일정이 있으면 해당 일정으로 타이머 진입
                                viewModel.loadScheduledSession(schedule)
                                onNavigateToTimer()
                            }
                        }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }

    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var sessionMinutes by remember { mutableStateOf("60") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("${selectedHour}시 몰입 세션 설정") },
            text = {
                Column {
                    TextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("어떤 작업에 몰입할까요?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = sessionMinutes,
                        onValueChange = { if (it.all { char -> char.isDigit() }) sessionMinutes = it },
                        label = { Text("총 작업 시간 (분)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = sessionMinutes.toIntOrNull() ?: 60
                    if (taskTitle.isNotBlank()) {
                        // 미래의 일정인 경우 저장만 하고, 오늘인 경우 바로 시작할지 선택 가능하지만
                        // 여기서는 스케줄에 추가하는 방향으로 구현
                        viewModel.addSchedule(taskTitle, minutes, selectedHour)
                        showAddTaskDialog = false
                    }
                }) {
                    Text("추가하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun TimeRow(
    hour: Int,
    schedule: ScheduleBlock?,
    isCurrent: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%02d:00", hour),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
            color = if (isCurrent && isRunning) MaterialTheme.colorScheme.primary else Color.Unspecified,
            fontWeight = if (isCurrent && isRunning) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        val containerColor = when {
            schedule == null -> Color.LightGray.copy(alpha = 0.2f)
            schedule.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
            isCurrent && isRunning -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.7f)
                .then(
                    if (isCurrent && isRunning) 
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    else Modifier
                )
                .background(containerColor, shape = MaterialTheme.shapes.small)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (schedule == null) {
                Text(
                    text = "블록 추가 가능",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (schedule.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (isCurrent && isRunning) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Running",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Text(
                        text = "${schedule.taskTitle} (${schedule.durationMinutes}분)",
                        color = if (schedule.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant 
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                        textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else null
                    )
                    
                    if (isCurrent && isRunning) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "진행 중",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        if (schedule == null) {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    }
}

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isTomorrow(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    return sdf.format(Date(millis))
}
