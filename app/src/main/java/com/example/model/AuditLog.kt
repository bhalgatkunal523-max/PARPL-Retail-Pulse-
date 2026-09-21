package com.example.model

data class AuditLog(
    val logId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val employeeId: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val action: String = "",
    val recordType: String = "",
    val affectedRecordId: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
