package com.example.adhdblockscheduler.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.adhdblockscheduler.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TimerService : Service() {

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _totalRemainingSeconds = MutableStateFlow(0)
    val totalRemainingSeconds = _totalRemainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _currentBlockIndex = MutableStateFlow(0)
    val currentBlockIndex = _currentBlockIndex.asStateFlow()

    // Configuration
    private var alarmIntervalMinutes = 15
    private var totalSecondsAtStart = 0
    private var taskTitle = "작업"
    private var vibrationEnabled = true
    private var onTransition: (String, Int, Boolean) -> Unit = { _, _, _ -> }
    private var onFinished: () -> Unit = {}

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationHelper.NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("Focus Flow")
            .setContentText("타이머 대기 중...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    fun setTimerConfig(
        interval: Int,
        totalSec: Int,
        title: String,
        vibrate: Boolean,
        onTransition: (String, Int, Boolean) -> Unit,
        onFinished: () -> Unit
    ) {
        this.alarmIntervalMinutes = interval
        this.totalSecondsAtStart = totalSec
        this.taskTitle = title
        this.vibrationEnabled = vibrate
        this.onTransition = onTransition
        this.onFinished = onFinished
    }

    fun startTimer(initialTotalRemaining: Int) {
        timerJob?.cancel()
        _isRunning.value = true
        _totalRemainingSeconds.value = initialTotalRemaining
        
        // 포그라운드 알림 즉시 업데이트 (요구사항 2: 작업명 포함)
        updateNotification()

        timerJob = serviceScope.launch {
            while (_totalRemainingSeconds.value > 0) {
                delay(1000L)
                val currentTotalRemaining = maxOf(0, _totalRemainingSeconds.value - 1)
                
                val sessionElapsedSeconds = totalSecondsAtStart - currentTotalRemaining
                val intervalSeconds = alarmIntervalMinutes * 60
                val newBlockIndex = sessionElapsedSeconds / intervalSeconds
                
                if (newBlockIndex != _currentBlockIndex.value && currentTotalRemaining > 0) {
                    val elapsedMinutes = (newBlockIndex) * alarmIntervalMinutes
                    onTransition(taskTitle, elapsedMinutes, false)
                    _currentBlockIndex.value = newBlockIndex
                }

                _totalRemainingSeconds.value = currentTotalRemaining
                _remainingSeconds.value = intervalSeconds - (sessionElapsedSeconds % intervalSeconds)
                
                // 매 초마다 알림 갱신 (요구사항 2: 남은 시간 실시간 표시)
                updateNotification()
            }

            _isRunning.value = false
            _totalRemainingSeconds.value = 0
            
            // 세션 종료 알림 (요구사항 4: 버그 수정)
            onTransition(taskTitle, totalSecondsAtStart / 60, true)

            onFinished()
            stopForeground(STOP_FOREGROUND_REMOVE) 
            stopSelf()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isRunning.value = false
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        _totalRemainingSeconds.value = 0
        _currentBlockIndex.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val minutes = _totalRemainingSeconds.value / 60
        val seconds = _totalRemainingSeconds.value % 60
        val timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        
        // 알림 클릭 시 타이머로 이동하도록 인텐트 설정 (요구사항 3)
        val intent = Intent(this, com.example.adhdblockscheduler.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "timer")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("몰입 중: $taskTitle") // 요구사항 2: 작업명 표시
            .setContentText("남은 시간: $timeText")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent) // 요구사항 3: 클릭 시 이동
            .setOnlyAlertOnce(true) // 매초 소리/진동 방지
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }
}
