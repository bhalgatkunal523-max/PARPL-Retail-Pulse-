package com.example.model

enum class LeadStatus {
    NEW,
    CONTACTED,
    FOLLOW_UP_REQUIRED,
    INTERESTED,
    CLOSED,
    LOST;

    val label: String
        get() = when (this) {
            NEW -> "New Lead"
            CONTACTED -> "Contacted"
            FOLLOW_UP_REQUIRED -> "Follow-up Required"
            INTERESTED -> "Interested"
            CLOSED -> "Closed / Billed"
            LOST -> "Lost"
        }
}

data class WalkInLead(
    val walkInId: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val productCategory: String = "",
    val productModel: String = "",
    val approximateBudget: Double = 0.0,
    val notes: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: LeadStatus = LeadStatus.NEW,
    val closedSaleId: String = "",
    val nextFollowUpTimestamp: Long? = null
)
