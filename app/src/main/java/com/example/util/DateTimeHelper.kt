package com.example.util

import com.example.model.AttendanceStatus
import com.example.model.DateFilterType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val hourMinute24Format = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun todayDateString(): String {
        return dateFormat.format(Date())
    }

    fun formatDate(timestamp: Long): String {
        return displayDateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    /**
     * Determines whether arrival is ON_TIME or LATE based on branch opening time (HH:mm)
     * and grace period in minutes.
     * Example: openingTime = "09:30", gracePeriod = 5.
     * Up to 09:35 is ON_TIME. 09:36 onwards is LATE.
     * Returns Pair(AttendanceStatus, minutesLate).
     */
    fun evaluateAttendanceStatus(
        timestamp: Long,
        openingTimeStr: String,
        gracePeriodMinutes: Int
    ): Pair<AttendanceStatus, Int> {
        return try {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val actualHour = cal.get(Calendar.HOUR_OF_DAY)
            val actualMinute = cal.get(Calendar.MINUTE)
            val actualTotalMinutes = actualHour * 60 + actualMinute

            val parts = openingTimeStr.split(":")
            val openingHour = parts[0].trim().toInt()
            val openingMin = parts[1].trim().toInt()
            val openingTotalMinutes = openingHour * 60 + openingMin

            val allowedTotalMinutes = openingTotalMinutes + gracePeriodMinutes

            if (actualTotalMinutes <= allowedTotalMinutes) {
                Pair(AttendanceStatus.ON_TIME, 0)
            } else {
                val lateMinutes = actualTotalMinutes - allowedTotalMinutes
                Pair(AttendanceStatus.LATE, lateMinutes)
            }
        } catch (e: Exception) {
            Pair(AttendanceStatus.ON_TIME, 0)
        }
    }

    /**
     * Checks if a given timestamp falls within the specified DateFilterType.
     */
    fun matchesFilter(timestamp: Long, filterType: DateFilterType): Boolean {
        if (filterType == DateFilterType.ALL_TIME) return true

        val recordCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        return when (filterType) {
            DateFilterType.TODAY -> {
                recordCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        recordCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
            }
            DateFilterType.YESTERDAY -> {
                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                recordCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                        recordCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
            }
            DateFilterType.THIS_WEEK -> {
                recordCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        recordCal.get(Calendar.WEEK_OF_YEAR) == nowCal.get(Calendar.WEEK_OF_YEAR)
            }
            DateFilterType.THIS_MONTH -> {
                recordCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        recordCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
            }
            DateFilterType.LAST_MONTH -> {
                val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                recordCal.get(Calendar.YEAR) == lastMonthCal.get(Calendar.YEAR) &&
                        recordCal.get(Calendar.MONTH) == lastMonthCal.get(Calendar.MONTH)
            }
            DateFilterType.ALL_TIME -> true
        }
    }
}
