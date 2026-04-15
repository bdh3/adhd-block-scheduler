package com.focusflow.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusflow.app.util.BlockType
import com.focusflow.app.util.NotificationHelper
import com.focusflow.app.util.VibrationPattern

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // [v1.7.6-fix] 구버전에서 예약된 '좀비 알람' 차단
        // 현재는 고정된 액션 "com.focusflow.app.ALARM_ACTION"만 사용합니다.
        val action = intent.action
        // [v1.8.2-fix] Doze 모드에서 프로세스가 새로 시작될 경우 isServiceRunning이 false일 수 있음.
        // 액션이 확실하다면 알람을 울리도록 보장하여 지연 현상 방지.
        if (action != "com.focusflow.app.ALARM_ACTION") return

        // [v1.8.2-fix] 리시버가 실행되는 동안 CPU가 잠들지 않도록 즉시 WakeLock 획득
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wl = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "FocusFlow:ReceiverWakeLock")
        wl.acquire(5000L)

        val taskTitle = intent.getStringExtra("taskTitle") ?: "작업"
        val elapsedMinutes = intent.getIntExtra("elapsedMinutes", 0)
        val isFinished = intent.getBooleanExtra("isFinished", false)
        val vibrationEnabled = intent.getBooleanExtra("vibrationEnabled", true)
        val soundEnabled = intent.getBooleanExtra("soundEnabled", true)
        val useFullScreen = intent.getBooleanExtra("useFullScreen", false)
        
        val blockTypeName = intent.getStringExtra("blockType") ?: BlockType.FOCUS.name
        val currentBlockType = try { BlockType.valueOf(blockTypeName) } catch (e: Exception) { BlockType.FOCUS }
        
        val focusPatternId = intent.getStringExtra("focusVibrationPatternId")
        val restPatternId = intent.getStringExtra("restVibrationPatternId")
        val finishPatternId = intent.getStringExtra("finishVibrationPatternId")
        val focusSoundId = intent.getStringExtra("focusSoundId") ?: "default"
        val restSoundId = intent.getStringExtra("restSoundId") ?: "default"
        val finishSoundId = intent.getStringExtra("finishSoundId") ?: "default"
        val ringtoneUri = intent.getStringExtra("ringtoneUri")
        
        val focusPattern = VibrationPattern.fromId(focusPatternId).pattern
        val restPattern = VibrationPattern.fromId(restPatternId).pattern
        val finishPattern = VibrationPattern.fromId(finishPatternId).pattern

        val notificationHelper = NotificationHelper.getInstance(context)
        notificationHelper.showBlockTransitionNotification(
            taskTitle = taskTitle,
            elapsedMinutes = elapsedMinutes,
            isFinished = isFinished,
            currentBlockType = currentBlockType,
            focusVibrationPattern = focusPattern,
            restVibrationPattern = restPattern,
            finishVibrationPattern = finishPattern,
            focusSoundId = focusSoundId,
            restSoundId = restSoundId,
            finishSoundId = finishSoundId,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
            ringtoneUri = ringtoneUri,
            useFullScreen = useFullScreen,
            isManualSkip = false // 알람 리시버를 통한 알람은 수동 넘기기가 아님
        )
    }
}
