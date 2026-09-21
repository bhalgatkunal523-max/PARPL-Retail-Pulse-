package com.example.model

enum class DateFilterType(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    ALL_TIME("All Time")
}

data class KpiMetrics(
    // Attendance
    val totalEmployees: Int = 0,
    val presentToday: Int = 0,
    val lateToday: Int = 0,
    val absentToday: Int = 0,
    val attendancePercentage: Double = 0.0,
    val averageArrivalTimeMinutes: Int = 0, // e.g. 575 -> 9:35 AM

    // Walk-ins
    val totalWalkIns: Int = 0,
    val newWalkIns: Int = 0,
    val contacted: Int = 0,
    val followUpRequired: Int = 0,
    val interested: Int = 0,
    val closed: Int = 0,
    val lost: Int = 0,

    // Follow-ups
    val followUpsCompleted: Int = 0,
    val pendingFollowUps: Int = 0,
    val overdueFollowUps: Int = 0,
    val followUpsDueToday: Int = 0,

    // Sales
    val totalConvertedSales: Int = 0,
    val totalBillValue: Double = 0.0,
    val averageBillValue: Double = 0.0,
    val conversionRate: Double = 0.0 // closed / total * 100
)

data class EmployeePerformance(
    val employeeId: String,
    val employeeName: String,
    val branchId: String,
    val branchName: String,
    val daysPresent: Int = 0,
    val lateArrivals: Int = 0,
    val attendancePercentage: Double = 0.0,
    val totalWalkIns: Int = 0,
    val closedWalkIns: Int = 0,
    val conversionRate: Double = 0.0,
    val totalSalesAmount: Double = 0.0,
    val pendingFollowUps: Int = 0
)

data class BranchPerformance(
    val branchId: String,
    val branchName: String,
    val employeeCount: Int = 0,
    val presentCount: Int = 0,
    val lateCount: Int = 0,
    val walkInCount: Int = 0,
    val closedCount: Int = 0,
    val conversionRate: Double = 0.0,
    val totalSalesAmount: Double = 0.0
)
