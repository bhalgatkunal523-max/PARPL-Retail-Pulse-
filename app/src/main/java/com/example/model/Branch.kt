package com.example.model

data class Branch(
    val branchId: String = "",
    val branchName: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geofenceRadius: Double = 100.0, // in meters, e.g. 100m
    val openingTime: String = "09:30", // HH:mm 24-hr format
    val closingTime: String = "21:30",
    val gracePeriodMinutes: Int = 5,
    val active: Boolean = true,
    val managerId: String = "",
    val managerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
