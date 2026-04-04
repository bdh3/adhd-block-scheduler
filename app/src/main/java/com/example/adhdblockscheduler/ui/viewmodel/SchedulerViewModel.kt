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
        settingsRepository.blockDurationMinutes
    ) { state, tasks, calendarSync, duration ->
        state.copy(
            tasks = tasks,
            calendarSyncEnabled = calendarSync,
            blockDuration = duration.toInt()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchedulerUiState()
    )

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.blockDurationMinutes.collect { duration ->
                generateDefaultBlocks(duration.toInt())
            }
        }
    }

    private fun generateDefaultBlocks(duration: Int) {
        val blocks = mutableListOf<TimeBlock>()
        val currentTime = System.currentTimeMillis()
        
        for (i in 0 until 3) {
            blocks.add(
                TimeBlock(
                    startTime = currentTime + (i * duration * 60 * 1000L),
                    durationMinutes = duration,
                    type = BlockType.FOCUS
                )
            )
        }
        blocks.add(
            TimeBlock(
                startTime = currentTime + (3 * duration * 60 * 1000L),
                durationMinutes = duration,
                type = BlockType.REST
            )
        )
        
        _uiState.update { it.copy(
            timeBlocks = blocks,
            remainingSeconds = blocks.firstOrNull()?.durationMinutes?.times(60) ?: 0
        ) }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        _uiState.update { it.copy(isRunning = true) }
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            onBlockFinished()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    private fun onBlockFinished() {
        val currentState = _uiState.value
        val currentIndex = currentState.currentBlockIndex
        val finishedBlock = currentState.timeBlocks[currentIndex]
        val finishedBlockType = finishedBlock.type
        val nextIndex = currentIndex + 1
        
        // 알림 및 진동 발생
        notificationHelper.showBlockFinishedNotification(finishedBlockType)

        // 통계 저장 및 캘린더 연동
        viewModelScope.launch {
            if (finishedBlockType == BlockType.FOCUS) {
                statsRepository.addFocusMinutes(finishedBlock.durationMinutes)
                
                // 캘린더 연동이 켜져있을 때만 기록
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

        if (nextIndex < updatedBlocks.size) {
            _uiState.update { it.copy(
                timeBlocks = updatedBlocks,
                currentBlockIndex = nextIndex,
                remainingSeconds = updatedBlocks[nextIndex].durationMinutes * 60,
                isRunning = false
            ) }
        } else {
            _uiState.update { it.copy(
                timeBlocks = updatedBlocks,
                isRunning = false
            ) }
        }
    }

    fun updateBlockDuration(minutes: Float) {
        viewModelScope.launch {
            settingsRepository.setBlockDurationMinutes(minutes)
        }
    }

    fun updateCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCalendarSyncEnabled(enabled)
        }
    }

    fun skipBlock() {
        pauseTimer()
        onBlockFinished()
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
    val currentBlockIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val calendarSyncEnabled: Boolean = false,
    val blockDuration: Int = 15
)
