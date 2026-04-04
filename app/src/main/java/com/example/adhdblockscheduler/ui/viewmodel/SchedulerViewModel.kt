package com.example.adhdblockscheduler.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.adhdblockscheduler.ADHDBlockSchedulerApplication
import com.example.adhdblockscheduler.data.prefs.SettingsRepository
import com.example.adhdblockscheduler.data.repository.StatsRepository
import com.example.adhdblockscheduler.data.repository.TaskRepository
import com.example.adhdblockscheduler.model.BlockType
import com.example.adhdblockscheduler.model.Task
import com.example.adhdblockscheduler.model.TimeBlock
import com.example.adhdblockscheduler.util.CalendarHelper
import com.example.adhdblockscheduler.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SchedulerViewModel(
    private val app: Application,
    private val repository: TaskRepository,
    private val statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(app) {
    
    private val notificationHelper = NotificationHelper(app)
    private val _uiState = MutableStateFlow(SchedulerUiState())
    
    val uiState: StateFlow<SchedulerUiState> = combine(
        _uiState,
        repository.allTasks,
        settingsRepository.calendarSyncEnabled,
        settingsRepository.vibrationEnabled,
        settingsRepository.blocksPerHour,
        settingsRepository.restMinutes
    ) { Array ->
        val state = Array[0] as SchedulerUiState
        val tasks = Array[1] as List<Task>
        val calendarSync = Array[2] as Boolean
        val vibration = Array[3] as Boolean
        val blocksPerHour = Array[4] as Int
        val restMin = Array[5] as Int

        // 전체 남은 시간 계산
        val currentBlockRemaining = state.remainingSeconds
        val futureBlocksRemaining = state.timeBlocks
            .drop(state.currentBlockIndex + 1)
            .sumOf { it.durationMinutes * 60 }
        val totalRemaining = currentBlockRemaining + futureBlocksRemaining

        state.copy(
            tasks = tasks,
            calendarSyncEnabled = calendarSync,
            vibrationEnabled = vibration,
            blocksPerHour = blocksPerHour,
            restMinutes = restMin,
            totalRemainingSeconds = totalRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchedulerUiState()
    )

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.blocksPerHour,
                settingsRepository.restMinutes
            ) { perHour, rest -> perHour to rest }.collect { (perHour, rest) ->
                if (!_uiState.value.isRunning) {
                    generateDefaultBlocks(perHour, rest)
                }
            }
        }
    }

    private fun generateDefaultBlocks(blocksPerHour: Int, restMinutes: Int) {
        val totalMinutes = 60
        val focusMinutes = totalMinutes - restMinutes
        val blockDuration = 60 / blocksPerHour // 1시간을 쪼개는 단위
        
        val blocks = mutableListOf<TimeBlock>()
        var currentTime = System.currentTimeMillis()

        // 1. 집중 블록 생성 (쪼개기 단위 기준)
        val focusBlocksCount = focusMinutes / blockDuration
        for (i in 0 until focusBlocksCount) {
            blocks.add(TimeBlock(startTime = currentTime, durationMinutes = blockDuration, type = BlockType.FOCUS))
            currentTime += blockDuration * 60 * 1000L
        }
        
        // 집중 시간의 나머지 자투리 시간이 있다면 추가
        val focusRemainder = focusMinutes % blockDuration
        if (focusRemainder > 0) {
            blocks.add(TimeBlock(startTime = currentTime, durationMinutes = focusRemainder, type = BlockType.FOCUS))
            currentTime += focusRemainder * 60 * 1000L
        }

        // 2. 휴식 블록 생성 (통째로 하나)
        if (restMinutes > 0) {
            blocks.add(TimeBlock(startTime = currentTime, durationMinutes = restMinutes, type = BlockType.REST))
        }
        
        _uiState.update { it.copy(
            timeBlocks = blocks,
            currentBlockIndex = 0,
            remainingSeconds = if (blocks.isNotEmpty()) blocks[0].durationMinutes * 60 else 0,
            totalRemainingSeconds = blocks.sumOf { it.durationMinutes * 60 }
        ) }
    }

    fun selectTask(taskId: String) {
        _uiState.update { it.copy(selectedTaskId = taskId) }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            if (_uiState.value.selectedTaskId == null && _uiState.value.tasks.isNotEmpty()) {
                // 할 일을 선택하지 않고 시작하면 첫 번째 할 일 자동 선택
                selectTask(_uiState.value.tasks[0].id)
            }
            startTimer()
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        _uiState.update { it.copy(isRunning = true) }
        
        timerJob = viewModelScope.launch {
            // 전체 1시간(또는 설정된 총 블록 시간)을 하나의 루프로 관리
            val totalSecondsAtStart = _uiState.value.timeBlocks.sumOf { it.durationMinutes * 60 }
            if (_uiState.value.totalRemainingSeconds <= 0) {
                _uiState.update { it.copy(totalRemainingSeconds = totalSecondsAtStart) }
            }

            val startTime = System.currentTimeMillis()
            val initialTotalRemaining = _uiState.value.totalRemainingSeconds

            while (_uiState.value.totalRemainingSeconds > 0) {
                delay(500L)
                val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                val currentTotalRemaining = maxOf(0, initialTotalRemaining - elapsedSeconds)
                
                // 현재 몇 번째 블록인지 계산
                val elapsedFromSessionStart = totalSecondsAtStart - currentTotalRemaining
                var accumulatedSeconds = 0
                var newBlockIndex = 0
                for ((index, block) in _uiState.value.timeBlocks.withIndex()) {
                    accumulatedSeconds += block.durationMinutes * 60
                    if (elapsedFromSessionStart < accumulatedSeconds) {
                        newBlockIndex = index
                        break
                    }
                }

                // 현재 블록 내 남은 시간 (알림 간격용)
                val currentBlockLimit = _uiState.value.timeBlocks.take(newBlockIndex + 1).sumOf { it.durationMinutes * 60 }
                val remainingInBlock = currentBlockLimit - elapsedFromSessionStart
                val currentBlockType = _uiState.value.timeBlocks.getOrNull(newBlockIndex)?.type

                // 알림 및 진동 처리
                // 1. 블록이 전환될 때 (집중 블록 쪼개기 마다 또는 집중->휴식 전환)
                if (newBlockIndex != _uiState.value.currentBlockIndex) {
                    val prevBlock = _uiState.value.timeBlocks[_uiState.value.currentBlockIndex]
                    val nextBlock = _uiState.value.timeBlocks.getOrNull(newBlockIndex)
                    
                    onBlockTransition(prevBlock.type, nextBlock?.type)
                    _uiState.update { it.copy(currentBlockIndex = newBlockIndex) }
                }

                _uiState.update { it.copy(
                    totalRemainingSeconds = currentTotalRemaining,
                    remainingSeconds = remainingInBlock
                ) }
            }

            // 전체 세션 종료
            _uiState.update { it.copy(isRunning = false, totalRemainingSeconds = 0) }
            onSessionFinished()
        }
    }

    private fun onBlockTransition(finishedType: BlockType, nextType: BlockType?) {
        notificationHelper.showBlockTransitionNotification(
            finishedType = finishedType,
            nextType = nextType,
            vibrationEnabled = _uiState.value.vibrationEnabled
        )
        
        // 통계 저장 등 기존 로직 수행
        if (finishedType == BlockType.FOCUS) {
            val duration = _uiState.value.timeBlocks[_uiState.value.currentBlockIndex].durationMinutes
            viewModelScope.launch {
                statsRepository.addFocusMinutes(duration)
            }
        }
    }

    private fun onSessionFinished() {
        // 모든 블록이 끝났을 때의 처리
        _uiState.update { it.copy(
            timeBlocks = it.timeBlocks.map { b -> b.copy(isCompleted = true) }
        ) }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    private fun onBlockFinished(
        showNotification: Boolean = true,
        finishedType: BlockType? = null,
        nextType: BlockType? = null
    ) {
        val currentState = _uiState.value
        val currentIndex = currentState.currentBlockIndex
        val finishedBlock = currentState.timeBlocks[currentIndex]
        val blockType = finishedType ?: finishedBlock.type
        
        // 블록 전환 알림 (자동 진행 중에도 알림 발생)
        if (showNotification) {
            notificationHelper.showBlockTransitionNotification(
                finishedType = blockType,
                nextType = nextType,
                vibrationEnabled = currentState.vibrationEnabled
            )
        }

        // 통계 저장 및 캘린더 연동
        viewModelScope.launch {
            if (blockType == BlockType.FOCUS) {
                statsRepository.addFocusMinutes(finishedBlock.durationMinutes)
                
                if (currentState.calendarSyncEnabled) {
                    CalendarHelper.addEventToCalendar(
                        context = app,
                        title = "집중 완료",
                        description = "ADHD 블록 스케줄러를 통한 집중 세션",
                        durationMinutes = finishedBlock.durationMinutes
                    )
                }
            }
        }

        val updatedBlocks = currentState.timeBlocks.mapIndexed { index, block ->
            if (index == currentIndex) block.copy(isCompleted = true) else block
        }

        _uiState.update { it.copy(timeBlocks = updatedBlocks) }
    }

    fun updateBlocksPerHour(count: Int) {
        viewModelScope.launch {
            settingsRepository.setBlocksPerHour(count)
        }
    }

    fun updateRestMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setRestMinutes(minutes)
        }
    }

    fun updateCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCalendarSyncEnabled(enabled)
        }
    }

    fun skipBlock() {
        val currentState = _uiState.value
        val currentIndex = currentState.currentBlockIndex
        val nextIndex = currentIndex + 1
        val nextBlock = currentState.timeBlocks.getOrNull(nextIndex)

        pauseTimer()
        onBlockFinished(
            showNotification = false,
            finishedType = currentState.timeBlocks[currentIndex].type,
            nextType = nextBlock?.type
        )

        if (nextBlock != null) {
            _uiState.update { it.copy(
                currentBlockIndex = nextIndex,
                remainingSeconds = nextBlock.durationMinutes * 60
            ) }
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            repository.insertTask(Task(title = title))
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updatedTask)
            if (updatedTask.isCompleted) {
                statsRepository.incrementTaskCount()
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
            // 즉시 UI 상태 반영 (데이터스토어 collect 전이라도 빠른 피드백을 위해)
            _uiState.update { it.copy(vibrationEnabled = enabled) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as ADHDBlockSchedulerApplication
                return SchedulerViewModel(
                    application,
                    application.taskRepository,
                    application.statsRepository,
                    application.settingsRepository
                ) as T
            }
        }
    }
}

data class SchedulerUiState(
    val timeBlocks: List<TimeBlock> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val selectedTaskId: String? = null, // 선택된 할 일 ID (String으로 수정)
    val currentBlockIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val totalRemainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val calendarSyncEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val blocksPerHour: Int = 4,
    val restMinutes: Int = 15
)
