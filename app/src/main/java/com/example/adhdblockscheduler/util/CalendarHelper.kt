package com.example.adhdblockscheduler.util

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.util.*

object CalendarHelper {
    fun addEventToCalendar(context: Context, title: String, description: String, startTime: Long, durationMinutes: Int) {
        val endTime = startTime + (durationMinutes * 60 * 1000L)

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTime)
            put(CalendarContract.Events.DTEND, endTime)
            put(CalendarContract.Events.TITLE, "[Focus Flow] $title")
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, 1) // 기본 캘린더 ID (보통 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
