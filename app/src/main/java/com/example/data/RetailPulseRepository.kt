package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import com.example.util.DateTimeHelper
import com.example.util.LocationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RetailPulseRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("retail_pulse_data_store", Context.MODE_PRIVATE)

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // State Flows
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile>(
        UserProfile(
            userId = "usr_owner_01",
            name = "Vikram Mehra (Owner)",
            email = "owner@retailpulse.com",
            employeeId = "OWNER-001",
            role = UserRole.OWNER,
            branchId = "",
            branchName = "All Branches",
            active = true
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _branches = MutableStateFlow<List<Branch>>(emptyList())
    val branches: StateFlow<List<Branch>> = _branches.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _walkIns = MutableStateFlow<List<WalkInLead>>(emptyList())
    val walkIns: StateFlow<List<WalkInLead>> = _walkIns.asStateFlow()

    private val _followUps = MutableStateFlow<List<FollowUp>>(emptyList())
    val followUps: StateFlow<List<FollowUp>> = _followUps.asStateFlow()

    private val _sales = MutableStateFlow<List<SaleRecord>>(emptyList())
    val sales: StateFlow<List<SaleRecord>> = _sales.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _correctionRequests = MutableStateFlow<List<CorrectionRequest>>(emptyList())
    val correctionRequests: StateFlow<List<CorrectionRequest>> = _correctionRequests.asStateFlow()

    init {
        loadOrInitializeData()
    }

    private fun loadOrInitializeData() {
        seedInitialData()
        loadFromLocalPrefs()
    }

    private fun seedInitialData() {
        // 1. Seed 13 Branches as requested: Branch 01 to Branch 13
        val initialBranches = mutableListOf<Branch>()
        val branchLocations = listOf(
            Triple("Connaught Place, New Delhi", 28.6315, 77.2167),
            Triple("Nehru Place Electronics Market, New Delhi", 28.5494, 77.2529),
            Triple("Loni Retail Center, Ghaziabad", 28.7512, 77.2889),
            Triple("South Extension Market, New Delhi", 28.5701, 77.2228),
            Triple("Lajpat Nagar Central, New Delhi", 28.5700, 77.2400),
            Triple("Sector 18 Noida Hub, Noida", 28.5708, 77.3261),
            Triple("MG Road Galleria, Gurugram", 28.4795, 77.0800),
            Triple("Cyber Hub Tech Arcade, Gurugram", 28.4950, 77.0895),
            Triple("Karol Bagh Electronics Row, New Delhi", 28.6515, 77.1906),
            Triple("Rohini City Center, New Delhi", 28.7166, 77.1158),
            Triple("Dwarka Sector 12 Market, New Delhi", 28.5921, 77.0460),
            Triple("Indirapuram Habitat Mall, Ghaziabad", 28.6415, 77.3712),
            Triple("Faridabad Crown Plaza, Faridabad", 28.4089, 77.3178)
        )

        for (i in 1..13) {
            val numStr = if (i < 10) "0$i" else "$i"
            val loc = branchLocations[i - 1]
            val branchId = "br_$numStr"
            val branchName = "Branch $numStr"

            val lat = prefs.getString("branch_${branchId}_lat", null)?.toDoubleOrNull() ?: loc.second
            val lon = prefs.getString("branch_${branchId}_lon", null)?.toDoubleOrNull() ?: loc.third
            val radius = prefs.getString("branch_${branchId}_radius", null)?.toDoubleOrNull() ?: 100.0

            initialBranches.add(
                Branch(
                    branchId = branchId,
                    branchName = branchName,
                    address = loc.first,
                    latitude = lat,
                    longitude = lon,
                    geofenceRadius = radius,
                    openingTime = "09:30",
                    closingTime = "21:30",
                    gracePeriodMinutes = 5,
                    active = true,
                    managerId = if (i <= 2) "usr_mgr_$numStr" else "",
                    managerName = when (i) {
                        1 -> "Rajesh Sharma"
                        2 -> "Priya Patel"
                        else -> "Unassigned"
                    },
                    createdAt = System.currentTimeMillis() - (86400000L * 30)
                )
            )
        }
        _branches.value = initialBranches

        // 2. Seed Users across roles: OWNER, BRANCH_MANAGER, EMPLOYEE
        val initialUsers = listOf(
            UserProfile(
                userId = "usr_owner_01",
                name = "Vikram Mehra",
                email = "vikram.mehra@retailpulse.com",
                employeeId = "OWNER-001",
                role = UserRole.OWNER,
                branchId = "",
                branchName = "All Branches",
                phone = "+91 98110 00100"
            ),
            UserProfile(
                userId = "usr_mgr_01",
                name = "Rajesh Sharma",
                email = "rajesh.cp@retailpulse.com",
                employeeId = "MGR-101",
                role = UserRole.BRANCH_MANAGER,
                branchId = "br_01",
                branchName = "Branch 01",
                phone = "+91 98110 00201"
            ),
            UserProfile(
                userId = "usr_mgr_02",
                name = "Priya Patel",
                email = "priya.np@retailpulse.com",
                employeeId = "MGR-201",
                role = UserRole.BRANCH_MANAGER,
                branchId = "br_02",
                branchName = "Branch 02",
                phone = "+91 98110 00202"
            ),
            UserProfile(
                userId = "usr_emp_101",
                name = "Amit Verma",
                email = "amit.v@retailpulse.com",
                employeeId = "EMP-101",
                role = UserRole.EMPLOYEE,
                branchId = "br_01",
                branchName = "Branch 01",
                phone = "+91 98110 00301"
            ),
            UserProfile(
                userId = "usr_emp_102",
                name = "Rahul Gupta",
                email = "rahul.g@retailpulse.com",
                employeeId = "EMP-102",
                role = UserRole.EMPLOYEE,
                branchId = "br_01",
                branchName = "Branch 01",
                phone = "+91 98110 00302"
            ),
            UserProfile(
                userId = "usr_emp_201",
                name = "Sneha Rao",
                email = "sneha.r@retailpulse.com",
                employeeId = "EMP-201",
                role = UserRole.EMPLOYEE,
                branchId = "br_02",
                branchName = "Branch 02",
                phone = "+91 98110 00303"
            ),
            UserProfile(
                userId = "usr_emp_301",
                name = "Karan Singh",
                email = "karan.s@retailpulse.com",
                employeeId = "EMP-301",
                role = UserRole.EMPLOYEE,
                branchId = "br_03",
                branchName = "Branch 03",
                phone = "+91 98110 00304"
            )
        )
        _users.value = initialUsers
        _currentUser.value = initialUsers[0] // Default to Owner

        // 3. Seed Walk-In Leads
        val now = System.currentTimeMillis()
        val initialWalkIns = listOf(
            WalkInLead(
                walkInId = "wk_001",
                customerName = "Rahul Sharma",
                customerMobile = "+91 98765 43210",
                productCategory = "Smart TVs",
                productModel = "Sony Bravia 55 inch OLED 4K",
                approximateBudget = 85000.0,
                notes = "Customer asked for exchange bonus on old 40 inch LED.",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                branchName = "Branch 01",
                createdAt = now - (86400000L * 2),
                status = LeadStatus.INTERESTED,
                nextFollowUpTimestamp = now + (86400000L * 1)
            ),
            WalkInLead(
                walkInId = "wk_002",
                customerName = "Ananya Roy",
                customerMobile = "+91 98123 45678",
                productCategory = "Laptops",
                productModel = "Apple MacBook Air M3 15-inch",
                approximateBudget = 125000.0,
                notes = "Comparing against Dell XPS 14. Requires AppleCare quote.",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                branchName = "Branch 01",
                createdAt = now - (86400000L * 1),
                status = LeadStatus.FOLLOW_UP_REQUIRED,
                nextFollowUpTimestamp = now // Due Today!
            ),
            WalkInLead(
                walkInId = "wk_003",
                customerName = "Deepak Kumar",
                customerMobile = "+91 97234 56789",
                productCategory = "Smartphones",
                productModel = "Samsung Galaxy S24 Ultra 512GB",
                approximateBudget = 130000.0,
                notes = "Purchased and billed via HDFC Credit Card.",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                branchName = "Branch 01",
                createdAt = now - (86400000L * 3),
                status = LeadStatus.CLOSED,
                closedSaleId = "sl_001"
            ),
            WalkInLead(
                walkInId = "wk_004",
                customerName = "Vikram Seth",
                customerMobile = "+91 99887 76655",
                productCategory = "Audio",
                productModel = "Bose QuietComfort Ultra",
                approximateBudget = 32000.0,
                notes = "Testing noise cancellation in-store.",
                employeeId = "usr_emp_102",
                employeeName = "Rahul Gupta",
                branchId = "br_01",
                branchName = "Branch 01",
                createdAt = now - (86400000L * 4),
                status = LeadStatus.CONTACTED,
                nextFollowUpTimestamp = now - 86400000L // Overdue!
            ),
            WalkInLead(
                walkInId = "wk_005",
                customerName = "Meera Nair",
                customerMobile = "+91 91234 56780",
                productCategory = "Home Appliances",
                productModel = "LG French Door Smart Refrigerator",
                approximateBudget = 95000.0,
                notes = "Checking kitchen dimensions at home.",
                employeeId = "usr_emp_201",
                employeeName = "Sneha Rao",
                branchId = "br_02",
                branchName = "Branch 02",
                createdAt = now - 3600000L * 5,
                status = LeadStatus.NEW
            )
        )
        _walkIns.value = initialWalkIns

        // 4. Seed Follow-ups
        val initialFollowUps = listOf(
            FollowUp(
                followUpId = "fu_001",
                walkInId = "wk_001",
                customerName = "Rahul Sharma",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                timestamp = now - (86400000L * 1),
                contactMethod = ContactMethod.PHONE_CALL,
                outcome = "Customer visited home, discussed with family, willing to proceed.",
                notes = "Agreed to hold TV until weekend.",
                nextFollowUpDate = DateTimeHelper.todayDateString(),
                statusAfter = LeadStatus.INTERESTED
            ),
            FollowUp(
                followUpId = "fu_002",
                walkInId = "wk_002",
                customerName = "Ananya Roy",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                timestamp = now - (3600000L * 12),
                contactMethod = ContactMethod.WHATSAPP,
                outcome = "Sent comparison sheet between MacBook Air M3 and Dell XPS.",
                notes = "Waiting for customer review today.",
                nextFollowUpDate = DateTimeHelper.todayDateString(),
                statusAfter = LeadStatus.FOLLOW_UP_REQUIRED
            )
        )
        _followUps.value = initialFollowUps

        // 5. Seed Initial Sale
        val initialSales = listOf(
            SaleRecord(
                saleId = "sl_001",
                walkInId = "wk_003",
                customerName = "Deepak Kumar",
                customerMobile = "+91 97234 56789",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                branchName = "Branch 01",
                invoiceNumber = "INV-2026-0891",
                billAmount = 124999.0,
                saleDate = DateTimeHelper.todayDateString(),
                productCategory = "Smartphones",
                productModel = "Samsung Galaxy S24 Ultra 512GB",
                quantity = 1,
                discount = 5000.0,
                notes = "Screen protector bundle included.",
                conversionTimestamp = now - 3600000L * 2
            )
        )
        _sales.value = initialSales

        // 6. Seed Attendance Records
        val todayStr = DateTimeHelper.todayDateString()
        val initialAttendance = listOf(
            AttendanceRecord(
                attendanceId = "att_001",
                employeeId = "usr_emp_101",
                employeeName = "Amit Verma",
                branchId = "br_01",
                branchName = "Branch 01",
                selfieUrl = "asset:///placeholder_selfie.jpg",
                latitude = 28.6315,
                longitude = 77.2167,
                timestamp = now - 3600000L * 4,
                attendanceStatus = AttendanceStatus.ON_TIME,
                minutesLate = 0,
                date = todayStr
            ),
            AttendanceRecord(
                attendanceId = "att_002",
                employeeId = "usr_emp_102",
                employeeName = "Rahul Gupta",
                branchId = "br_01",
                branchName = "Branch 01",
                selfieUrl = "asset:///placeholder_selfie.jpg",
                latitude = 28.6316,
                longitude = 77.2168,
                timestamp = now - 3600000L * 3 + 1200000L,
                attendanceStatus = AttendanceStatus.LATE,
                minutesLate = 18,
                date = todayStr
            )
        )
        _attendanceRecords.value = initialAttendance

        // 7. Seed Initial Audit Log
        val initialLogs = listOf(
            AuditLog(
                logId = "log_001",
                userId = "usr_owner_01",
                userName = "Vikram Mehra",
                userRole = "OWNER",
                branchId = "br_01",
                branchName = "Branch 01",
                action = "SYSTEM_INITIALIZATION",
                recordType = "SYSTEM",
                affectedRecordId = "sys_init",
                previousValue = "None",
                newValue = "Seeded 13 branches and initial roles",
                timestamp = now
            )
        )
        _auditLogs.value = initialLogs

        saveToLocalPrefs()
        prefs.edit().putBoolean("is_initialized", true).apply()

        // Also sync initial state to Firestore if available
        syncAllToFirestore()
    }

    private fun saveToLocalPrefs() {
        // Persist to SharedPreferences as JSON strings for guaranteed offline resilience
        val editor = prefs.edit()
        editor.putString("current_user_id", _currentUser.value.userId)
        editor.apply()
    }

    private fun loadFromLocalPrefs() {
        // Defaults if re-opened
        val currentUserId = prefs.getString("current_user_id", "usr_owner_01")
        val found = _users.value.find { it.userId == currentUserId }
        if (found != null) {
            _currentUser.value = found
        }
    }

    private fun syncAllToFirestore() {
        firestore?.let { db ->
            repositoryScope.launch {
                try {
                    // Sync branches
                    for (b in _branches.value) {
                        db.collection("branches").document(b.branchId).set(b, SetOptions.merge())
                    }
                    // Sync users
                    for (u in _users.value) {
                        db.collection("users").document(u.userId).set(u, SetOptions.merge())
                    }
                } catch (e: Exception) {
                    // Graceful fallback to local persistence
                }
            }
        }
    }

    // --- Authentication & User Operations ---

    fun switchUser(user: UserProfile) {
        _currentUser.value = user
        prefs.edit().putString("current_user_id", user.userId).apply()
    }

    fun addUser(newUser: UserProfile): Result<UserProfile> {
        val updated = _users.value.toMutableList()
        updated.add(newUser)
        _users.value = updated
        firestore?.collection("users")?.document(newUser.userId)?.set(newUser)

        logAction(
            action = "EMPLOYEE_CREATION",
            recordType = "USER",
            recordId = newUser.userId,
            prev = "None",
            newV = "${newUser.name} (${newUser.role.name})",
            user = _currentUser.value
        )
        return Result.success(newUser)
    }

    fun updateUser(user: UserProfile): Result<UserProfile> {
        val updated = _users.value.map { if (it.userId == user.userId) user else it }
        _users.value = updated
        if (_currentUser.value.userId == user.userId) {
            _currentUser.value = user
        }
        firestore?.collection("users")?.document(user.userId)?.set(user, SetOptions.merge())
        return Result.success(user)
    }

    // --- Branch Operations ---

    fun addBranch(newBranch: Branch): Result<Branch> {
        val updated = _branches.value.toMutableList()
        updated.add(newBranch)
        _branches.value = updated
        firestore?.collection("branches")?.document(newBranch.branchId)?.set(newBranch)

        logAction(
            action = "BRANCH_CREATION",
            recordType = "BRANCH",
            recordId = newBranch.branchId,
            prev = "None",
            newV = newBranch.branchName,
            user = _currentUser.value
        )
        return Result.success(newBranch)
    }

    fun updateBranch(branch: Branch): Result<Branch> {
        val prev = _branches.value.find { it.branchId == branch.branchId }
        val updated = _branches.value.map { if (it.branchId == branch.branchId) branch else it }
        _branches.value = updated

        // Persist to local preferences for offline resilience & testing
        prefs.edit()
            .putString("branch_${branch.branchId}_name", branch.branchName)
            .putString("branch_${branch.branchId}_lat", branch.latitude.toString())
            .putString("branch_${branch.branchId}_lon", branch.longitude.toString())
            .putString("branch_${branch.branchId}_radius", branch.geofenceRadius.toString())
            .putString("branch_${branch.branchId}_open", branch.openingTime)
            .putString("branch_${branch.branchId}_close", branch.closingTime)
            .putInt("branch_${branch.branchId}_grace", branch.gracePeriodMinutes)
            .apply()

        firestore?.collection("branches")?.document(branch.branchId)?.set(branch, SetOptions.merge())

        logAction(
            action = "BRANCH_UPDATE",
            recordType = "BRANCH",
            recordId = branch.branchId,
            prev = "${prev?.branchName ?: ""} (Radius: ${prev?.geofenceRadius}m)",
            newV = "${branch.branchName} (Radius: ${branch.geofenceRadius}m)",
            user = _currentUser.value
        )
        return Result.success(branch)
    }

    fun toggleBranchStatus(branchId: String, active: Boolean) {
        val branch = _branches.value.find { it.branchId == branchId } ?: return
        val updatedBranch = branch.copy(active = active)
        updateBranch(updatedBranch)
    }

    // --- Attendance Operations ---

    fun markAttendance(
        user: UserProfile,
        selfieUri: String,
        currentLat: Double,
        currentLon: Double
    ): Result<AttendanceRecord> {
        // 1. Identify branch automatically from user profile
        val branch = _branches.value.find { it.branchId == user.branchId }
            ?: return Result.failure(IllegalStateException("User is not assigned to a valid branch."))

        // 2. Check geofence
        val isInside = LocationHelper.isWithinGeofence(
            userLat = currentLat,
            userLon = currentLon,
            branchLat = branch.latitude,
            branchLon = branch.longitude,
            radiusMeters = branch.geofenceRadius
        )

        val distanceMeters = LocationHelper.calculateDistanceMeters(
            currentLat, currentLon, branch.latitude, branch.longitude
        )

        if (!isInside) {
            val distFormatted = String.format("%.0f", distanceMeters)
            val allowedFormatted = String.format("%.0f", branch.geofenceRadius)
            return Result.failure(
                SecurityException(
                    "You are outside the permitted branch geofence for ${branch.branchName}.\n" +
                            "Current distance: ${distFormatted}m (Allowed radius: ${allowedFormatted}m)."
                )
            )
        }

        // 3. Evaluate timing
        val now = System.currentTimeMillis()
        val (status, minutesLate) = DateTimeHelper.evaluateAttendanceStatus(
            timestamp = now,
            openingTimeStr = branch.openingTime,
            gracePeriodMinutes = branch.gracePeriodMinutes
        )

        // 4. Create immutable attendance record
        val attendance = AttendanceRecord(
            attendanceId = "att_${UUID.randomUUID().toString().take(8)}",
            employeeId = user.employeeId.ifEmpty { user.userId },
            employeeName = user.name,
            branchId = branch.branchId,
            branchName = branch.branchName,
            selfieUrl = selfieUri,
            latitude = currentLat,
            longitude = currentLon,
            timestamp = now,
            attendanceStatus = status,
            minutesLate = minutesLate,
            date = DateTimeHelper.todayDateString()
        )

        val updatedList = _attendanceRecords.value.toMutableList()
        updatedList.add(0, attendance)
        _attendanceRecords.value = updatedList

        firestore?.collection("attendance")?.document(attendance.attendanceId)?.set(attendance)

        logAction(
            action = "ATTENDANCE_MARKED",
            recordType = "ATTENDANCE",
            recordId = attendance.attendanceId,
            prev = "None",
            newV = "Status: ${status.name}, Minutes Late: $minutesLate",
            user = user
        )

        return Result.success(attendance)
    }

    // --- Walk-in Operations ---

    fun createWalkIn(
        customerName: String,
        customerMobile: String,
        category: String,
        model: String,
        budget: Double,
        notes: String,
        user: UserProfile
    ): Result<WalkInLead> {
        val branch = _branches.value.find { it.branchId == user.branchId }
            ?: return Result.failure(IllegalStateException("No assigned branch found for this employee."))

        val walkIn = WalkInLead(
            walkInId = "wk_${UUID.randomUUID().toString().take(8)}",
            customerName = customerName.trim(),
            customerMobile = customerMobile.trim(),
            productCategory = category.trim(),
            productModel = model.trim(),
            approximateBudget = budget,
            notes = notes.trim(),
            employeeId = user.employeeId.ifEmpty { user.userId },
            employeeName = user.name,
            branchId = branch.branchId,
            branchName = branch.branchName,
            createdAt = System.currentTimeMillis(),
            status = LeadStatus.NEW
        )

        val updated = _walkIns.value.toMutableList()
        updated.add(0, walkIn)
        _walkIns.value = updated

        firestore?.collection("walkIns")?.document(walkIn.walkInId)?.set(walkIn)

        logAction(
            action = "WALKIN_CREATED",
            recordType = "WALK_IN",
            recordId = walkIn.walkInId,
            prev = "None",
            newV = "Customer: ${walkIn.customerName} (${walkIn.customerMobile})",
            user = user
        )

        return Result.success(walkIn)
    }

    fun updateLeadStatus(
        walkInId: String,
        newStatus: LeadStatus,
        user: UserProfile
    ): Result<WalkInLead> {
        val lead = _walkIns.value.find { it.walkInId == walkInId }
            ?: return Result.failure(IllegalArgumentException("Lead not found"))

        val prevStatus = lead.status
        val updatedLead = lead.copy(status = newStatus)

        _walkIns.value = _walkIns.value.map { if (it.walkInId == walkInId) updatedLead else it }
        firestore?.collection("walkIns")?.document(walkInId)?.set(updatedLead, SetOptions.merge())

        logAction(
            action = "CUSTOMER_STATUS_CHANGE",
            recordType = "WALK_IN",
            recordId = walkInId,
            prev = prevStatus.name,
            newV = newStatus.name,
            user = user
        )

        return Result.success(updatedLead)
    }

    // --- Follow-up Operations ---

    fun addFollowUp(
        walkInId: String,
        contactMethod: ContactMethod,
        outcome: String,
        notes: String,
        nextFollowUpDate: String,
        statusAfter: LeadStatus,
        user: UserProfile
    ): Result<FollowUp> {
        val lead = _walkIns.value.find { it.walkInId == walkInId }
            ?: return Result.failure(IllegalArgumentException("Walk-in lead not found"))

        val followUp = FollowUp(
            followUpId = "fu_${UUID.randomUUID().toString().take(8)}",
            walkInId = walkInId,
            customerName = lead.customerName,
            employeeId = user.employeeId.ifEmpty { user.userId },
            employeeName = user.name,
            branchId = lead.branchId,
            timestamp = System.currentTimeMillis(),
            contactMethod = contactMethod,
            outcome = outcome.trim(),
            notes = notes.trim(),
            nextFollowUpDate = nextFollowUpDate.trim(),
            statusAfter = statusAfter
        )

        val updatedFollowUps = _followUps.value.toMutableList()
        updatedFollowUps.add(0, followUp)
        _followUps.value = updatedFollowUps

        // Update lead status and next follow-up date
        updateLeadStatus(walkInId, statusAfter, user)

        firestore?.collection("followUps")?.document(followUp.followUpId)?.set(followUp)

        logAction(
            action = "FOLLOWUP_RECORDED",
            recordType = "FOLLOW_UP",
            recordId = followUp.followUpId,
            prev = lead.status.name,
            newV = "Method: ${contactMethod.name}, Next: $nextFollowUpDate",
            user = user
        )

        return Result.success(followUp)
    }

    // --- Sale Operations ---

    fun checkDuplicateInvoice(invoiceNumber: String, branchId: String): Boolean {
        return _sales.value.any {
            it.branchId == branchId && it.invoiceNumber.equals(invoiceNumber.trim(), ignoreCase = true)
        }
    }

    fun closeSaleAndLead(
        walkInId: String,
        invoiceNumber: String,
        billAmount: Double,
        saleDate: String,
        productCategory: String,
        productModel: String,
        quantity: Int,
        discount: Double,
        notes: String,
        user: UserProfile
    ): Result<SaleRecord> {
        val lead = _walkIns.value.find { it.walkInId == walkInId }
            ?: return Result.failure(IllegalArgumentException("Walk-in lead not found"))

        if (invoiceNumber.isBlank() || billAmount <= 0 || saleDate.isBlank() || productCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("Required sale fields are missing or invalid."))
        }

        val saleId = "sl_${UUID.randomUUID().toString().take(8)}"
        val sale = SaleRecord(
            saleId = saleId,
            walkInId = walkInId,
            customerName = lead.customerName,
            customerMobile = lead.customerMobile,
            employeeId = user.employeeId.ifEmpty { user.userId },
            employeeName = user.name,
            branchId = lead.branchId,
            branchName = lead.branchName,
            invoiceNumber = invoiceNumber.trim(),
            billAmount = billAmount,
            saleDate = saleDate.trim(),
            productCategory = productCategory.trim(),
            productModel = productModel.trim(),
            quantity = quantity,
            discount = discount,
            notes = notes.trim(),
            conversionTimestamp = System.currentTimeMillis()
        )

        val updatedSales = _sales.value.toMutableList()
        updatedSales.add(0, sale)
        _sales.value = updatedSales

        // Update lead to CLOSED with closedSaleId
        val closedLead = lead.copy(status = LeadStatus.CLOSED, closedSaleId = saleId)
        _walkIns.value = _walkIns.value.map { if (it.walkInId == walkInId) closedLead else it }

        firestore?.collection("sales")?.document(sale.saleId)?.set(sale)
        firestore?.collection("walkIns")?.document(walkInId)?.set(closedLead, SetOptions.merge())

        logAction(
            action = "SALE_RECORDED",
            recordType = "SALE",
            recordId = sale.saleId,
            prev = "Lead: ${lead.status.name}",
            newV = "Closed: Inv #${sale.invoiceNumber}, ₹${sale.billAmount}",
            user = user
        )

        return Result.success(sale)
    }

    // --- Correction Requests ---

    fun submitCorrectionRequest(
        requestType: CorrectionType,
        targetRecordId: String,
        originalValue: String,
        requestedValue: String,
        reason: String,
        user: UserProfile
    ): Result<CorrectionRequest> {
        val branch = _branches.value.find { it.branchId == user.branchId }
        val req = CorrectionRequest(
            requestId = "cor_${UUID.randomUUID().toString().take(8)}",
            requestType = requestType,
            targetRecordId = targetRecordId,
            employeeId = user.employeeId.ifEmpty { user.userId },
            employeeName = user.name,
            branchId = user.branchId,
            branchName = branch?.branchName ?: user.branchName,
            originalValue = originalValue,
            requestedValue = requestedValue,
            reason = reason,
            status = CorrectionStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        val updated = _correctionRequests.value.toMutableList()
        updated.add(0, req)
        _correctionRequests.value = updated

        firestore?.collection("correctionRequests")?.document(req.requestId)?.set(req)

        logAction(
            action = if (requestType == CorrectionType.ATTENDANCE) "ATTENDANCE_CORRECTION_REQUEST" else "SALE_CORRECTION_REQUEST",
            recordType = "CORRECTION_REQUEST",
            recordId = req.requestId,
            prev = originalValue,
            newV = requestedValue,
            user = user
        )

        return Result.success(req)
    }

    fun reviewCorrectionRequest(
        requestId: String,
        status: CorrectionStatus,
        notes: String,
        reviewer: UserProfile
    ): Result<CorrectionRequest> {
        val req = _correctionRequests.value.find { it.requestId == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found"))

        val updated = req.copy(
            status = status,
            reviewedBy = reviewer.userId,
            reviewerName = reviewer.name,
            reviewTimestamp = System.currentTimeMillis(),
            reviewNotes = notes
        )

        _correctionRequests.value = _correctionRequests.value.map { if (it.requestId == requestId) updated else it }
        firestore?.collection("correctionRequests")?.document(requestId)?.set(updated, SetOptions.merge())

        val actionName = when (status) {
            CorrectionStatus.APPROVED -> if (req.requestType == CorrectionType.ATTENDANCE) "ATTENDANCE_CORRECTION_APPROVAL" else "SALE_CORRECTION_APPROVAL"
            CorrectionStatus.REJECTED -> if (req.requestType == CorrectionType.ATTENDANCE) "ATTENDANCE_CORRECTION_REJECTION" else "SALE_CORRECTION_REJECTION"
            else -> "CORRECTION_REVIEW"
        }

        logAction(
            action = actionName,
            recordType = "CORRECTION_REQUEST",
            recordId = requestId,
            prev = req.status.name,
            newV = status.name,
            user = reviewer
        )

        return Result.success(updated)
    }

    // --- Audit Logging ---

    fun logAction(
        action: String,
        recordType: String,
        recordId: String,
        prev: String,
        newV: String,
        user: UserProfile
    ) {
        val log = AuditLog(
            logId = "log_${UUID.randomUUID().toString().take(8)}",
            userId = user.userId,
            userName = user.name,
            userRole = user.role.name,
            employeeId = user.employeeId,
            branchId = user.branchId,
            branchName = user.branchName,
            action = action,
            recordType = recordType,
            affectedRecordId = recordId,
            previousValue = prev,
            newValue = newV,
            timestamp = System.currentTimeMillis()
        )

        val updated = _auditLogs.value.toMutableList()
        updated.add(0, log)
        _auditLogs.value = updated

        firestore?.collection("auditLogs")?.document(log.logId)?.set(log)
    }
}
