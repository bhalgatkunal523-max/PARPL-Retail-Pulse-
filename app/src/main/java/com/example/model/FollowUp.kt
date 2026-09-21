package com.example.model

enum class ContactMethod {
    PHONE_CALL,
    WHATSAPP,
    IN_STORE_VISIT,
    OTHER;

    val label: String
        get() = when (this) {
            PHONE_CALL -> "Phone Call"
            WHATSAPP -> "WhatsApp"
            IN_STORE_VISIT -> "In-Store Visit"
            OTHER -> "Other"
        }
}

data class FollowUp(
    val followUpId: String = "",
    val walkInId: String = "",
    val customerName: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val branchId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val contactMethod: ContactMethod = ContactMethod.PHONE_CALL,
    val outcome: String = "",
    val notes: String = "",
    val nextFollowUpDate: String = "", // e.g. "2026-09-24"
    val statusAfter: LeadStatus = LeadStatus.CONTACTED
)
