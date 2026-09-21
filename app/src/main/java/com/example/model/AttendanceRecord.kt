package com.example.model

enum class AttendanceStatus {
    ON_TIME,
    LATE,
    REJECTED;

    val label: String
        get() = when (this) {
            ON_TIME -> "ON TIME"
            LATE -> "LATE"
            REJECTED -> "REJECTED"
        }
}

data class AttendanceRecord(
    val attendanceId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val selfieUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val attendanceStatus: AttendanceStatus = AttendanceStatus.ON_TIME,
    val minutesLate: Int = 0,
    val date: String = "" // format "YYYY-MM-DD"
)
