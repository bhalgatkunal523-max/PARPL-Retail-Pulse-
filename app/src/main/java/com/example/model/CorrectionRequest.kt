package com.example.model

enum class CorrectionType {
    ATTENDANCE,
    SALE;

    val label: String
        get() = when (this) {
            ATTENDANCE -> "Attendance Correction"
            SALE -> "Sale / Invoice Correction"
        }
}

enum class CorrectionStatus {
    PENDING,
    APPROVED,
    REJECTED;

    val label: String
        get() = when (this) {
            PENDING -> "Pending Review"
            APPROVED -> "Approved"
            REJECTED -> "Rejected"
        }
}

data class CorrectionRequest(
    val requestId: String = "",
    val requestType: CorrectionType = CorrectionType.ATTENDANCE,
    val targetRecordId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val originalValue: String = "",
    val requestedValue: String = "",
    val reason: String = "",
    val status: CorrectionStatus = CorrectionStatus.PENDING,
    val reviewedBy: String = "",
    val reviewerName: String = "",
    val reviewTimestamp: Long? = null,
    val reviewNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
