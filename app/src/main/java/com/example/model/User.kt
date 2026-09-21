package com.example.model

enum class UserRole {
    OWNER,
    BRANCH_MANAGER,
    EMPLOYEE;

    val displayName: String
        get() = when (this) {
            OWNER -> "Business Owner"
            BRANCH_MANAGER -> "Branch Manager"
            EMPLOYEE -> "Retail Sales Staff"
        }
}

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val employeeId: String = "",
    val role: UserRole = UserRole.EMPLOYEE,
    val branchId: String = "",
    val branchName: String = "",
    val active: Boolean = true,
    val phone: String = ""
)
