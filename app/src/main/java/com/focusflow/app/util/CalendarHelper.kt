package com.focusflow.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import java.util.TimeZone

class CalendarHelper(private val context: Context) {
    fun addEventToCalendar(title: String, startTimeMillis: Long, endTimeMillis: Long) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTimeMillis)
            put(CalendarContract.Events.DTEND, endTimeMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, "Focus Flow에서 기록됨")
            put(CalendarContract.Events.CALENDAR_ID, 1) // 기본 캘린더
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }
}
