package com.example.model

data class SaleRecord(
    val saleId: String = "",
    val walkInId: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val branchId: String = "",
    val branchName: String = "",
    val invoiceNumber: String = "",
    val billAmount: Double = 0.0,
    val saleDate: String = "", // e.g. "2026-09-20"
    val productCategory: String = "",
    val productModel: String = "",
    val quantity: Int = 1,
    val discount: Double = 0.0,
    val notes: String = "",
    val conversionTimestamp: Long = System.currentTimeMillis()
)
