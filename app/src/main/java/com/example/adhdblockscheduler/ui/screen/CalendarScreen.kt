package com.example.adhdblockscheduler.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var isMonthlyView by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = {
                        viewModel.selectDate(System.currentTimeMillis())
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "오늘")
                    }
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            add(if (isMonthlyView) Calendar.MONTH else Calendar.DAY_OF_YEAR, -1)
                        }
                        viewModel.selectDate(cal.timeInMillis)
                    }) {
                        Text("<")
                    }
                    Text(
                        text = if (isMonthlyView) formatMonth(selectedDate)
                               else formatDate(selectedDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            add(if (isMonthlyView) Calendar.MONTH else Calendar.DAY_OF_YEAR, 1)
                        }
                        viewModel.selectDate(cal.timeInMillis)
                    }) {
                        Text(">")
                    }
                }
            )
        }
    ) { padding ->
        if (isMonthlyView) {
            MonthlyCalendarView(
                selectedDate = selectedDate,
                onDateSelected = { 
                    viewModel.selectDate(it)
                    isMonthlyView = false
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            DailyTimelineView(
                uiState = uiState,
                selectedDate = selectedDate,
                onAddSchedule = { hour, minutes, duration ->
                    selectedHour = hour
                    // 다이얼로그에서 사용할 초기값 설정 로직 보완 가능
                    showAddTaskDialog = true
                },
                onLoadSchedule = { schedule ->
                    viewModel.loadScheduledSession(schedule)
                    onNavigateToTimer()
                },
                onToggleBlock = { blockTime ->
                    viewModel.toggleBlock(blockTime)
                },
                onClearSelection = { viewModel.clearSelectedBlocks() },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showAddTaskDialog) {
        val selectedBlockList = uiState.selectedBlocks.sorted()
        val initialDuration = if (selectedBlockList.isNotEmpty()) selectedBlockList.size * 15 else 60
        val startCal = Calendar.getInstance().apply { 
            timeInMillis = selectedBlockList.firstOrNull() ?: System.currentTimeMillis() 
        }
        val initialHour = startCal.get(Calendar.HOUR_OF_DAY)
        val initialMinute = startCal.get(Calendar.MINUTE)

        var taskTitle by remember { mutableStateOf("") }
        var durationMinutes by remember { mutableIntStateOf(initialDuration) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("새 몰입 세션 추가") },
            text = {
                Column {
                    Text(
                        text = String.format(Locale.getDefault(), "시작 시간: %02d:%02d", initialHour, initialMinute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("어떤 작업을 할까요?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("총 소요 시간: ${durationMinutes}분", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = durationMinutes.toFloat(),
                        onValueChange = { durationMinutes = it.toInt() },
                        valueRange = 15f..240f,
                        steps = 14
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (taskTitle.isNotBlank()) {
                        viewModel.addSchedule(taskTitle, durationMinutes, initialHour, initialMinute)
                        viewModel.clearSelectedBlocks()
                        showAddTaskDialog = false
                    }
                }) {
                    Text("일정 추가")
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
fun MonthlyCalendarView(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = selectedDate
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthStartDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val days = mutableListOf<Long?>()
    for (i in 0 until monthStartDayOfWeek) days.add(null)
    for (i in 1..daysInMonth) {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, i)
        days.add(cal.timeInMillis)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (day == "일") Color.Red else if (day == "토") Color.Blue else Color.Unspecified
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            items(days) { dateMillis ->
                if (dateMillis != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val isSelected = isSameDay(dateMillis, selectedDate)
                    val isToday = isToday(dateMillis)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else if (isToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { onDateSelected(dateMillis) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun DailyTimelineView(
    uiState: com.example.adhdblockscheduler.ui.viewmodel.SchedulerUiState,
    selectedDate: Long,
    onAddSchedule: (Int, Int, Int) -> Unit,
    onLoadSchedule: (ScheduleBlock) -> Unit,
    onToggleBlock: (Long) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.selectedBlocks.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${uiState.selectedBlocks.size}개 블록 선택됨",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "총 ${uiState.selectedBlocks.size * 15}분 세션",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row {
                        TextButton(onClick = onClearSelection) {
                            Text("취소")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { 
                            val firstBlock = uiState.selectedBlocks.minOrNull() ?: return@Button
                            calendar.timeInMillis = firstBlock
                            onAddSchedule(
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                uiState.selectedBlocks.size * 15
                            ) 
                        }) {
                            Text("세션 생성")
                        }
                    }
                }
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items((0..23).toList()) { hour ->
                Column {
                    (0..3).forEach { quarter ->
                        val blockStartTime = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, quarter * 15)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val schedule = uiState.dailySchedules.find { 
                            val sCal = Calendar.getInstance().apply { timeInMillis = it.startTimeMillis }
                            val sHour = sCal.get(Calendar.HOUR_OF_DAY)
                            val sMin = sCal.get(Calendar.MINUTE)
                            val duration = it.durationMinutes
                            
                            val blockMin = hour * 60 + quarter * 15
                            val sStartMin = sHour * 60 + sMin
                            val sEndMin = sStartMin + duration
                            
                            blockMin >= sStartMin && blockMin < sEndMin
                        }

                        val isSelected = uiState.selectedBlocks.contains(blockStartTime)

                        TimeBlockRow(
                            hour = hour,
                            quarter = quarter,
                            schedule = schedule,
                            isSelected = isSelected,
                            isCurrent = schedule?.id != null && schedule.id == uiState.currentScheduleId,
                            isRunning = uiState.isRunning,
                            onClick = {
                                if (schedule == null) {
                                    onToggleBlock(blockStartTime)
                                } else {
                                    onLoadSchedule(schedule)
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun TimeBlockRow(
    hour: Int,
    quarter: Int,
    schedule: ScheduleBlock?,
    isSelected: Boolean,
    isCurrent: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (quarter == 0) String.format(Locale.getDefault(), "%02d:00", hour) else "",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(50.dp),
            color = if (isCurrent && isRunning) MaterialTheme.colorScheme.primary else Color.Gray
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        val containerColor = when {
            schedule == null -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.1f)
            schedule.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
            isCurrent && isRunning -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.8f)
                .background(containerColor, shape = MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (schedule != null && quarter == 0) { // 스케줄 시작점에만 텍스트 표시 (단순화)
                 Text(
                    text = schedule.taskTitle,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
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

private fun isSameDay(m1: Long, m2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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

private fun formatMonth(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM", Locale.getDefault())
    return sdf.format(Date(millis))
}
