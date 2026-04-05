package com.example.adhdblockscheduler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.adhdblockscheduler.util.NotificationHelper

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("taskTitle") ?: "작업"
        val elapsedMinutes = intent.getIntExtra("elapsedMinutes", 0)
        val isFinished = intent.getBooleanExtra("isFinished", false)
        val vibrationEnabled = intent.getBooleanExtra("vibrationEnabled", true)

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showBlockTransitionNotification(
            taskTitle = taskTitle,
            elapsedMinutes = elapsedMinutes,
            isFinished = isFinished,
            vibrationEnabled = vibrationEnabled
        )
    }
}
