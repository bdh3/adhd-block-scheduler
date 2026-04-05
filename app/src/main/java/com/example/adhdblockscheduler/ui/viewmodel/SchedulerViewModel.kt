package com.example.adhdblockscheduler.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.adhdblockscheduler.model.BlockType
import com.example.adhdblockscheduler.model.ScheduleBlock
import com.example.adhdblockscheduler.model.Task
import com.example.adhdblockscheduler.model.TimeBlock
import com.example.adhdblockscheduler.data.repository.ScheduleRepository
import com.example.adhdblockscheduler.data.prefs.SettingsRepository
import com.example.adhdblockscheduler.data.repository.StatsRepository
import com.example.adhdblockscheduler.data.repository.TaskRepository
import com.example.adhdblockscheduler.service.TimerService
import com.example.adhdblockscheduler.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class SchedulerUiState(
    val tasks: List<Task> = emptyList(),
    val selectedTaskId: String? = null,
    val timeBlocks: List<TimeBlock> = emptyList(),
    val currentBlockIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val sessionTotalMinutes: Int = 60,
    val totalRemainingSeconds: Int = 0,
    val vibrationEnabled: Boolean = true,
    val alarmIntervalMinutes: Int = 15,
    val calendarSyncEnabled: Boolean = false,
    val selectedBlocks: Set<Long> = emptySet(),
    val dailySchedules: List<ScheduleBlock> = emptyList(),
    val allSchedules: List<ScheduleBlock> = emptyList(),
    val currentScheduleId: String? = null,
    val selectionAnchor: Long? = null,
    val activeSessionInterval: Int = 15,
    val restMinutes: Int = 0
)

class SchedulerViewModel(
    private val app: Application,
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val statsRepository: StatsRepository,
    private val scheduleRepository: ScheduleRepository
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(SchedulerUiState())
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val notificationHelper = NotificationHelper(app)
    
    private var timerService: TimerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true

            viewModelScope.launch {
                timerService?.totalRemainingSeconds?.collect { seconds ->
                    _uiState.update { it.copy(totalRemainingSeconds = seconds) }
                }
            }

            viewModelScope.launch {
                timerService?.remainingSeconds?.collect { seconds ->
                    _uiState.update { it.copy(remainingSeconds = seconds) }
                }
            }

            viewModelScope.launch {
                timerService?.currentBlockIndex?.collect { index ->
                    _uiState.update { it.copy(currentBlockIndex = index) }
                }
            }

            viewModelScope.launch {
                timerService?.isRunning?.collect { running ->
                    _uiState.update { it.copy(isRunning = running) }
                    
                    // 서비스가 실행 중이거나 남은 시간이 있는데 블록이 비어있다면 즉시 복구
                    val state = _uiState.value
                    if ((running || state.totalRemainingSeconds > 0) && state.timeBlocks.isEmpty()) {
                        generateDefaultBlocks(state.activeSessionInterval, state.sessionTotalMinutes)
                    }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            isBound = false
            timerService = null
        }
    }

    init {
        val intent = Intent(app, TimerService::class.java)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // 설정값 실시간 구독 및 반영
        viewModelScope.launch {
            settingsRepository.alarmIntervalMinutes.collect { interval ->
                _uiState.update { state ->
                    val isSessionActive = state.isRunning || state.totalRemainingSeconds > 0
                    if (!isSessionActive) {
                        // 세션이 활성화되지 않았을 때만 설정값에 맞춰 세션 인터벌과 블록 미리보기 갱신
                        state.copy(
                            alarmIntervalMinutes = interval,
                            activeSessionInterval = interval
                        )
                    } else {
                        state.copy(alarmIntervalMinutes = interval)
                    }
                }
                // 블록 미리보기 갱신 (세션 비활성 시)
                if (!_uiState.value.isRunning && _uiState.value.totalRemainingSeconds <= 0) {
                    generateDefaultBlocks(interval, _uiState.value.sessionTotalMinutes)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.vibrationEnabled.collect { enabled ->
                _uiState.update { it.copy(vibrationEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.restMinutes.collect { minutes ->
                _uiState.update { it.copy(restMinutes = minutes) }
                // 세션 비활성 시 블록 미리보기 갱신
                if (!_uiState.value.isRunning && _uiState.value.totalRemainingSeconds <= 0) {
                    generateDefaultBlocks(_uiState.value.alarmIntervalMinutes, _uiState.value.sessionTotalMinutes)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            app.unbindService(connection)
            isBound = false
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SchedulerUiState> = combine(
        _uiState,
        _selectedDate.flatMapLatest { dateMillis ->
            val today = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            combine(
                repository.getTasksForDate(today),
                scheduleRepository.getSchedulesForDay(today)
            ) { tasks, schedules ->
                val combined = tasks.toMutableList()
                schedules.forEach { schedule ->
                    val cal = Calendar.getInstance().apply { timeInMillis = schedule.startTimeMillis }
                    val endCal = Calendar.getInstance().apply { 
                        timeInMillis = schedule.startTimeMillis + schedule.durationMinutes * 60 * 1000L 
                    }
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d ~ %02d:%02d", 
                        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                        endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))
                    
                    val displayTitle = "${schedule.taskTitle} $timeStr"
                    val scheduleId = "sched_${schedule.id}"
                    
                    val existingIndex = combined.indexOfFirst { it.id == scheduleId }
                    val task = Task(
                        id = scheduleId, 
                        title = displayTitle, 
                        scheduledDateMillis = today,
                        isCompleted = schedule.isCompleted,
                        startTimeMillis = schedule.startTimeMillis
                    )
                    
                    if (existingIndex != -1) {
                        combined[existingIndex] = task
                    } else {
                        combined.add(task)
                    }
                }
                combined.sortedWith(compareBy({ it.startTimeMillis == 0L }, { it.startTimeMillis }, { it.createdAt }))
            }
        },
        settingsRepository.vibrationEnabled,
        settingsRepository.alarmIntervalMinutes,
        _selectedDate.flatMapLatest { scheduleRepository.getSchedulesForDay(it) },
        scheduleRepository.getAllSchedules()
    ) { params ->
        val state = params[0] as SchedulerUiState
        @Suppress("UNCHECKED_CAST")
        val tasks = params[1] as List<Task>
        val vibration = params[2] as Boolean
        val alarmInterval = params[3] as Int
        @Suppress("UNCHECKED_CAST")
        val dailySchedules = params[4] as List<ScheduleBlock>
        @Suppress("UNCHECKED_CAST")
        val allSchedules = params[5] as List<ScheduleBlock>

        val isSessionActive = state.isRunning || (state.totalRemainingSeconds > 0)
        
        val filteredTasks = if (isSessionActive && state.selectedTaskId != null) {
            tasks.filter { it.id == state.selectedTaskId }
        } else {
            tasks
        }

        val effectiveAlarmInterval = if (isSessionActive) {
            state.activeSessionInterval
        } else {
            alarmInterval
        }

        state.copy(
            tasks = filteredTasks,
            vibrationEnabled = vibration,
            alarmIntervalMinutes = effectiveAlarmInterval,
            dailySchedules = dailySchedules,
            allSchedules = allSchedules
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchedulerUiState()
    )

    private fun generateDefaultBlocks(intervalMinutes: Int, totalMinutes: Int) {
        val blocks = mutableListOf<TimeBlock>()
        val restMin = _uiState.value.restMinutes
        
        if (restMin <= 0) {
            // 기존 로직: 집중 블록만 생성
            val numBlocks = (totalMinutes + intervalMinutes - 1) / intervalMinutes
            for (i in 0 until numBlocks) {
                blocks.add(TimeBlock(
                    startTime = i * intervalMinutes * 60L,
                    durationMinutes = intervalMinutes,
                    type = BlockType.FOCUS
                ))
            }
        } else {
            // 집중/휴식 교차 로직
            var currentMinutes = 0
            while (currentMinutes < totalMinutes) {
                // 집중 블록 추가
                val focusDuration = if (currentMinutes + intervalMinutes > totalMinutes) {
                    totalMinutes - currentMinutes
                } else {
                    intervalMinutes
                }
                blocks.add(TimeBlock(
                    startTime = currentMinutes * 60L,
                    durationMinutes = focusDuration,
                    type = BlockType.FOCUS
                ))
                currentMinutes += focusDuration
                
                // 휴식 블록 추가 (남은 시간이 있다면)
                if (currentMinutes < totalMinutes) {
                    val restDuration = if (currentMinutes + restMin > totalMinutes) {
                        totalMinutes - currentMinutes
                    } else {
                        restMin
                    }
                    blocks.add(TimeBlock(
                        startTime = currentMinutes * 60L,
                        durationMinutes = restDuration,
                        type = BlockType.REST
                    ))
                    currentMinutes += restDuration
                }
            }
        }
        _uiState.update { it.copy(timeBlocks = blocks) }
    }

    fun addSchedule(
        taskTitle: String, 
        startTimeHour: Int, 
        startTimeMinute: Int, 
        durationMinutes: Int, 
        startNewSession: Boolean = false,
        intervalMinutes: Int = _uiState.value.alarmIntervalMinutes,
        restMinutes: Int = _uiState.value.restMinutes
    ) {
        viewModelScope.launch {
            val startTime = Calendar.getInstance().apply {
                timeInMillis = _selectedDate.value
                set(Calendar.HOUR_OF_DAY, startTimeHour)
                set(Calendar.MINUTE, startTimeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val schedule = ScheduleBlock(
                taskTitle = taskTitle,
                startTimeMillis = startTime,
                durationMinutes = durationMinutes,
                intervalMinutes = intervalMinutes,
                restMinutes = restMinutes
            )
            val id = scheduleRepository.insertSchedule(schedule)
            if (startNewSession) {
                loadScheduledSession(schedule.copy(id = id.toString()))
            }
        }
    }

    fun loadScheduledSession(schedule: ScheduleBlock) {
        if (_uiState.value.isRunning) return
        
        val taskInterval = schedule.intervalMinutes
        val taskRest = schedule.restMinutes
        
        _uiState.update { it.copy(
            selectedTaskId = "sched_${schedule.id}",
            sessionTotalMinutes = schedule.durationMinutes,
            currentScheduleId = schedule.id,
            activeSessionInterval = taskInterval,
            restMinutes = taskRest,
            remainingSeconds = taskInterval * 60,
            totalRemainingSeconds = schedule.durationMinutes * 60
        ) }
        
        generateDefaultBlocks(taskInterval, schedule.durationMinutes)
    }

    fun startNewSession(taskTitle: String, totalMinutes: Int, hourOfDay: Int? = null) {
        if (_uiState.value.isRunning) return
        
        viewModelScope.launch {
            val startTime = Calendar.getInstance().apply {
                timeInMillis = _selectedDate.value
                if (hourOfDay != null) set(Calendar.HOUR_OF_DAY, hourOfDay)
                else {
                    val now = Calendar.getInstance()
                    set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                }
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val newSchedule = ScheduleBlock(
                taskTitle = taskTitle,
                startTimeMillis = startTime,
                durationMinutes = totalMinutes
            )
            val id = scheduleRepository.insertSchedule(newSchedule)
            val scheduleWithId = newSchedule.copy(id = id.toString())
            
            val currentInterval = _uiState.value.alarmIntervalMinutes
            _uiState.update { it.copy(
                selectedTaskId = "sched_${scheduleWithId.id}",
                currentScheduleId = scheduleWithId.id,
                sessionTotalMinutes = totalMinutes,
                activeSessionInterval = currentInterval,
                totalRemainingSeconds = totalMinutes * 60
            ) }
            
            generateDefaultBlocks(currentInterval, totalMinutes)
        }
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    fun selectTask(taskId: String?) {
        _uiState.update { state ->
            val isSessionActive = state.isRunning || (state.totalRemainingSeconds > 0)
            if (isSessionActive && state.selectedTaskId != null) {
                return@update state
            }

            if (state.selectedTaskId == taskId) {
                state.copy(selectedTaskId = null, currentScheduleId = null)
            } else {
                val scheduleId = if (taskId?.startsWith("sched_") == true) taskId.removePrefix("sched_") else null
                state.copy(selectedTaskId = taskId, currentScheduleId = scheduleId)
            }
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            repository.insertTask(Task(title = title, scheduledDateMillis = _selectedDate.value))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.id.startsWith("sched_")) {
                val scheduleId = task.id.removePrefix("sched_")
                scheduleRepository.getScheduleById(scheduleId)?.let {
                    scheduleRepository.deleteSchedule(it)
                }
            } else {
                repository.deleteTask(task)
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (task.id.startsWith("sched_")) {
                val scheduleId = task.id.removePrefix("sched_")
                scheduleRepository.getScheduleById(scheduleId)?.let { current ->
                    scheduleRepository.updateSchedule(current.copy(isCompleted = !current.isCompleted))
                }
            } else {
                repository.updateTask(task.copy(isCompleted = !task.isCompleted))
            }
        }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun stopTimer() {
        timerService?.stopTimer()
        _uiState.update { it.copy(
            isRunning = false,
            totalRemainingSeconds = 0,
            currentBlockIndex = 0,
            remainingSeconds = 0
        ) }
    }

    fun startTimer() {
        val state = _uiState.value
        val currentInterval = state.alarmIntervalMinutes // 최신 설정값 사용

        if (state.totalRemainingSeconds <= 0) {
            val totalSeconds = state.sessionTotalMinutes * 60
            
            // 시작 시점의 인터벌을 세션용으로 박제하고 블록 재생성
            _uiState.update { it.copy(
                totalRemainingSeconds = totalSeconds,
                activeSessionInterval = currentInterval
            ) }
            generateDefaultBlocks(currentInterval, state.sessionTotalMinutes)
            
            timerService?.setTimerConfig(
                interval = currentInterval,
                totalSec = totalSeconds,
                title = state.tasks.find { it.id == state.selectedTaskId }?.title ?: "작업",
                vibrate = state.vibrationEnabled,
                onTransition = { title, elapsed, finished -> onBlockTransition(title, elapsed, finished) },
                onFinished = { onSessionFinished() }
            )
            timerService?.startTimer(totalSeconds)
        } else {
            // 재개 시에는 기존 박제된 activeSessionInterval 유지
            timerService?.setTimerConfig(
                interval = state.activeSessionInterval,
                rest = state.restMinutes,
                totalSec = state.sessionTotalMinutes * 60,
                title = state.tasks.find { it.id == state.selectedTaskId }?.title ?: "작업",
                vibrate = state.vibrationEnabled,
                onTransition = { title, elapsed, finished -> onBlockTransition(title, elapsed, finished) },
                onFinished = { onSessionFinished() }
            )
            timerService?.startTimer(state.totalRemainingSeconds)
        }
    }

    private fun pauseTimer() {
        timerService?.pauseTimer()
    }

    fun skipBlock() {
        val state = _uiState.value
        if (!state.isRunning) return
        
        val intervalSeconds = state.activeSessionInterval * 60
        val nextBlockIndex = state.currentBlockIndex + 1
        val totalBlocks = state.timeBlocks.size
        
        if (nextBlockIndex < totalBlocks) {
            val newTotalRemainingSeconds = (totalBlocks - nextBlockIndex) * intervalSeconds
            _uiState.update { it.copy(
                currentBlockIndex = nextBlockIndex,
                remainingSeconds = intervalSeconds,
                totalRemainingSeconds = newTotalRemainingSeconds
            ) }
            timerService?.setTimerConfig(
                interval = state.activeSessionInterval,
                rest = state.restMinutes,
                totalSec = state.sessionTotalMinutes * 60,
                title = state.tasks.find { it.id == state.selectedTaskId }?.title ?: "작업",
                vibrate = state.vibrationEnabled,
                onTransition = { title, elapsed, finished -> onBlockTransition(title, elapsed, finished) },
                onFinished = { onSessionFinished() }
            )
            timerService?.startTimer(newTotalRemainingSeconds)
        } else {
            onSessionFinished()
        }
    }

    fun onBlockTransition(taskTitle: String, elapsedMinutes: Int, isFinished: Boolean) {
        notificationHelper.showBlockTransitionNotification(
            taskTitle, 
            elapsedMinutes, 
            isFinished, 
            _uiState.value.vibrationEnabled
        )
    }

    fun onSessionFinished() {
        timerService?.stopTimer()
        val currentScheduleId = _uiState.value.currentScheduleId
        if (currentScheduleId != null) {
            viewModelScope.launch {
                scheduleRepository.getScheduleById(currentScheduleId)?.let {
                    scheduleRepository.updateSchedule(it.copy(isCompleted = true))
                }
            }
        }
        _uiState.update { it.copy(
            isRunning = false,
            totalRemainingSeconds = 0,
            currentBlockIndex = 0,
            remainingSeconds = 0
        ) }
    }

    fun saveSettings(interval: Int, rest: Int, vibration: Boolean, calendarSync: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAlarmIntervalMinutes(interval)
            settingsRepository.setRestMinutes(rest)
            settingsRepository.setVibrationEnabled(vibration)
            settingsRepository.setCalendarSyncEnabled(calendarSync)
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun requestIgnoreBatteryOptimizations() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${app.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(intent)
        }
    }

    fun deleteSchedule(schedule: ScheduleBlock) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule)
        }
    }

    fun updateSchedule(schedule: ScheduleBlock, newTitle: String) {
        viewModelScope.launch {
            val updated = schedule.copy(taskTitle = newTitle)
            scheduleRepository.updateSchedule(updated)
        }
    }

    fun toggleBlock(startTimeMillis: Long) {
        _uiState.update { state ->
            val anchor = state.selectionAnchor
            if (anchor == null) {
                state.copy(selectedBlocks = setOf(startTimeMillis), selectionAnchor = startTimeMillis)
            } else {
                val start = minOf(anchor, startTimeMillis)
                val end = maxOf(anchor, startTimeMillis)
                val newRange = mutableSetOf<Long>()
                var current = start
                while (current <= end) {
                    newRange.add(current)
                    current += 15 * 60 * 1000L
                }
                state.copy(selectedBlocks = newRange)
            }
        }
    }

    fun clearSelectedBlocks() {
        _uiState.update { it.copy(selectedBlocks = emptySet(), selectionAnchor = null) }
    }

    companion object {
        fun Factory(
            app: Application,
            repository: TaskRepository,
            settingsRepository: SettingsRepository,
            statsRepository: StatsRepository,
            scheduleRepository: ScheduleRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SchedulerViewModel(app, repository, settingsRepository, statsRepository, scheduleRepository) as T
            }
        }
    }
}
