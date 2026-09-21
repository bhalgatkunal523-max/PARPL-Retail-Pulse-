package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RetailPulseRepository
import com.example.model.*
import com.example.util.DateTimeHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = RetailPulseRepository(application.applicationContext)

    // Current authenticated user profile
    val currentUser = repository.currentUser
    val allUsers = repository.users
    val branches = repository.branches

    // Selected Branch Filter for Owner (empty string = ALL BRANCHES)
    private val _selectedBranchFilter = MutableStateFlow<String>("")
    val selectedBranchFilter: StateFlow<String> = _selectedBranchFilter.asStateFlow()

    // Date Range Filter
    private val _selectedDateFilter = MutableStateFlow<DateFilterType>(DateFilterType.THIS_MONTH)
    val selectedDateFilter: StateFlow<DateFilterType> = _selectedDateFilter.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Screen Tab in Employee or Management mode
    private val _currentScreen = MutableStateFlow<String>("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Selected Walk-In for Detail View
    private val _selectedWalkIn = MutableStateFlow<WalkInLead?>(null)
    val selectedWalkIn: StateFlow<WalkInLead?> = _selectedWalkIn.asStateFlow()

    // Feedback Toast / SnackBar state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Automatically sync branch filter when user changes role
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user.role == UserRole.BRANCH_MANAGER) {
                    _selectedBranchFilter.value = user.branchId
                } else if (user.role == UserRole.EMPLOYEE) {
                    _selectedBranchFilter.value = user.branchId
                }
            }
        }
    }

    fun setBranchFilter(branchId: String) {
        // Enforce role permission: Only OWNER can change branch filter freely
        if (currentUser.value.role == UserRole.OWNER) {
            _selectedBranchFilter.value = branchId
        }
    }

    fun setDateFilter(filter: DateFilterType) {
        _selectedDateFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectWalkIn(lead: WalkInLead?) {
        _selectedWalkIn.value = lead
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun switchUser(user: UserProfile) {
        repository.switchUser(user)
        if (user.role == UserRole.BRANCH_MANAGER || user.role == UserRole.EMPLOYEE) {
            _selectedBranchFilter.value = user.branchId
        } else {
            _selectedBranchFilter.value = "" // ALL BRANCHES for Owner
        }
        _currentScreen.value = "home"
    }

    // --- Role-Scoped Data Streams ---

    // 1. Scoped Walk-Ins
    val scopedWalkIns: StateFlow<List<WalkInLead>> = combine(
        repository.walkIns,
        currentUser,
        selectedBranchFilter,
        searchQuery
    ) { allLeads, user, branchFilter, query ->
        val permittedLeads = when (user.role) {
            UserRole.OWNER -> {
                if (branchFilter.isNotEmpty()) {
                    allLeads.filter { it.branchId == branchFilter }
                } else {
                    allLeads
                }
            }
            UserRole.BRANCH_MANAGER -> {
                allLeads.filter { it.branchId == user.branchId }
            }
            UserRole.EMPLOYEE -> {
                val empId = user.employeeId.ifEmpty { user.userId }
                allLeads.filter { it.employeeId == empId && it.branchId == user.branchId }
            }
        }

        if (query.isBlank()) {
            permittedLeads
        } else {
            permittedLeads.filter { lead ->
                lead.customerName.contains(query, ignoreCase = true) ||
                        lead.customerMobile.contains(query, ignoreCase = true) ||
                        lead.productModel.contains(query, ignoreCase = true) ||
                        lead.employeeName.contains(query, ignoreCase = true) ||
                        lead.branchName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Scoped Attendance Records
    val scopedAttendance: StateFlow<List<AttendanceRecord>> = combine(
        repository.attendanceRecords,
        currentUser,
        selectedBranchFilter
    ) { allAtt, user, branchFilter ->
        when (user.role) {
            UserRole.OWNER -> {
                if (branchFilter.isNotEmpty()) {
                    allAtt.filter { it.branchId == branchFilter }
                } else {
                    allAtt
                }
            }
            UserRole.BRANCH_MANAGER -> {
                allAtt.filter { it.branchId == user.branchId }
            }
            UserRole.EMPLOYEE -> {
                val empId = user.employeeId.ifEmpty { user.userId }
                allAtt.filter { it.employeeId == empId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Today's attendance for current employee
    val todayEmployeeAttendance: StateFlow<AttendanceRecord?> = combine(
        repository.attendanceRecords,
        currentUser
    ) { records, user ->
        val todayStr = DateTimeHelper.todayDateString()
        val empId = user.employeeId.ifEmpty { user.userId }
        records.find { it.employeeId == empId && it.date == todayStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 4. Scoped Follow-Ups
    val scopedFollowUps: StateFlow<List<FollowUp>> = combine(
        repository.followUps,
        currentUser,
        selectedBranchFilter
    ) { allFollowUps, user, branchFilter ->
        when (user.role) {
            UserRole.OWNER -> {
                if (branchFilter.isNotEmpty()) {
                    allFollowUps.filter { it.branchId == branchFilter }
                } else {
                    allFollowUps
                }
            }
            UserRole.BRANCH_MANAGER -> {
                allFollowUps.filter { it.branchId == user.branchId }
            }
            UserRole.EMPLOYEE -> {
                val empId = user.employeeId.ifEmpty { user.userId }
                allFollowUps.filter { it.employeeId == empId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Scoped Sales
    val scopedSales: StateFlow<List<SaleRecord>> = combine(
        repository.sales,
        currentUser,
        selectedBranchFilter
    ) { allSales, user, branchFilter ->
        when (user.role) {
            UserRole.OWNER -> {
                if (branchFilter.isNotEmpty()) {
                    allSales.filter { it.branchId == branchFilter }
                } else {
                    allSales
                }
            }
            UserRole.BRANCH_MANAGER -> {
                allSales.filter { it.branchId == user.branchId }
            }
            UserRole.EMPLOYEE -> {
                val empId = user.employeeId.ifEmpty { user.userId }
                allSales.filter { it.employeeId == empId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Scoped Correction Requests
    val scopedCorrectionRequests: StateFlow<List<CorrectionRequest>> = combine(
        repository.correctionRequests,
        currentUser,
        selectedBranchFilter
    ) { allReqs, user, branchFilter ->
        when (user.role) {
            UserRole.OWNER -> {
                if (branchFilter.isNotEmpty()) {
                    allReqs.filter { it.branchId == branchFilter }
                } else {
                    allReqs
                }
            }
            UserRole.BRANCH_MANAGER -> {
                allReqs.filter { it.branchId == user.branchId }
            }
            UserRole.EMPLOYEE -> {
                val empId = user.employeeId.ifEmpty { user.userId }
                allReqs.filter { it.employeeId == empId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class FilterContext(val branchFilter: String, val dateFilter: DateFilterType)
    data class ActivityBundle(
        val walkIns: List<WalkInLead>,
        val followUps: List<FollowUp>,
        val sales: List<SaleRecord>
    )

    private val filterContext: Flow<FilterContext> = combine(
        selectedBranchFilter,
        selectedDateFilter
    ) { b, d -> FilterContext(b, d) }

    private val activityBundle: Flow<ActivityBundle> = combine(
        repository.walkIns,
        repository.followUps,
        repository.sales
    ) { w, f, s -> ActivityBundle(w, f, s) }

    // 7. Overall Dashboard KPIs
    val dashboardKpi: StateFlow<KpiMetrics> = combine(
        repository.attendanceRecords,
        repository.users,
        activityBundle,
        filterContext
    ) { attList, userList, activity, filters ->
        val branchFilter = filters.branchFilter
        val dateFilter = filters.dateFilter
        val walkInList = activity.walkIns
        val followUpList = activity.followUps
        val saleList = activity.sales

        // 1. Filter by branch if applicable
        val bFilteredUsers = if (branchFilter.isNotEmpty()) userList.filter { it.branchId == branchFilter && it.role == UserRole.EMPLOYEE } else userList.filter { it.role == UserRole.EMPLOYEE }
        val bFilteredAtt = if (branchFilter.isNotEmpty()) attList.filter { it.branchId == branchFilter } else attList
        val bFilteredWalkIns = if (branchFilter.isNotEmpty()) walkInList.filter { it.branchId == branchFilter } else walkInList
        val bFilteredFollowUps = if (branchFilter.isNotEmpty()) followUpList.filter { it.branchId == branchFilter } else followUpList
        val bFilteredSales = if (branchFilter.isNotEmpty()) saleList.filter { it.branchId == branchFilter } else saleList

        // 2. Filter by date filter
        val todayStr = DateTimeHelper.todayDateString()
        val todayAtt = bFilteredAtt.filter { it.date == todayStr }
        val dateFilteredWalkIns = bFilteredWalkIns.filter { DateTimeHelper.matchesFilter(it.createdAt, dateFilter) }
        val dateFilteredSales = bFilteredSales.filter { DateTimeHelper.matchesFilter(it.conversionTimestamp, dateFilter) }
        val dateFilteredFollowUps = bFilteredFollowUps.filter { DateTimeHelper.matchesFilter(it.timestamp, dateFilter) }

        val totalEmployees = bFilteredUsers.size.coerceAtLeast(1)
        val presentToday = todayAtt.count { it.attendanceStatus != AttendanceStatus.REJECTED }
        val lateToday = todayAtt.count { it.attendanceStatus == AttendanceStatus.LATE }
        val absentToday = (totalEmployees - presentToday).coerceAtLeast(0)
        val attendancePct = (presentToday.toDouble() / totalEmployees) * 100.0

        val totalWalkIns = dateFilteredWalkIns.size
        val newWk = dateFilteredWalkIns.count { it.status == LeadStatus.NEW }
        val contacted = dateFilteredWalkIns.count { it.status == LeadStatus.CONTACTED }
        val followUpReq = dateFilteredWalkIns.count { it.status == LeadStatus.FOLLOW_UP_REQUIRED }
        val interested = dateFilteredWalkIns.count { it.status == LeadStatus.INTERESTED }
        val closed = dateFilteredWalkIns.count { it.status == LeadStatus.CLOSED }
        val lost = dateFilteredWalkIns.count { it.status == LeadStatus.LOST }

        val totalBillVal = dateFilteredSales.sumOf { it.billAmount }
        val avgBill = if (closed > 0) totalBillVal / closed else 0.0
        val conversion = if (totalWalkIns > 0) (closed.toDouble() / totalWalkIns) * 100.0 else 0.0

        val pendingFu = bFilteredWalkIns.count { it.status == LeadStatus.FOLLOW_UP_REQUIRED || it.status == LeadStatus.INTERESTED }
        val overdueFu = bFilteredWalkIns.count { it.nextFollowUpTimestamp != null && it.nextFollowUpTimestamp!! < System.currentTimeMillis() && it.status != LeadStatus.CLOSED && it.status != LeadStatus.LOST }
        val dueTodayFu = bFilteredWalkIns.count { it.status == LeadStatus.FOLLOW_UP_REQUIRED }

        KpiMetrics(
            totalEmployees = totalEmployees,
            presentToday = presentToday,
            lateToday = lateToday,
            absentToday = absentToday,
            attendancePercentage = attendancePct,
            totalWalkIns = totalWalkIns,
            newWalkIns = newWk,
            contacted = contacted,
            followUpRequired = followUpReq,
            interested = interested,
            closed = closed,
            lost = lost,
            followUpsCompleted = dateFilteredFollowUps.size,
            pendingFollowUps = pendingFu,
            overdueFollowUps = overdueFu,
            followUpsDueToday = dueTodayFu,
            totalConvertedSales = dateFilteredSales.size,
            totalBillValue = totalBillVal,
            averageBillValue = avgBill,
            conversionRate = conversion
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KpiMetrics())

    // 8. Employee-wise Performance
    val employeePerformanceList: StateFlow<List<EmployeePerformance>> = combine(
        repository.users,
        repository.attendanceRecords,
        activityBundle,
        filterContext
    ) { users, att, activity, filters ->
        val branchFilter = filters.branchFilter
        val dateFilter = filters.dateFilter
        val walkIns = activity.walkIns
        val sales = activity.sales

        val employees = users.filter { it.role == UserRole.EMPLOYEE }
            .filter { branchFilter.isEmpty() || it.branchId == branchFilter }

        employees.map { emp ->
            val empId: String = if (emp.employeeId.isNotEmpty()) emp.employeeId else emp.userId
            val empAtt = att.filter { it.employeeId == empId && DateTimeHelper.matchesFilter(it.timestamp, dateFilter) }
            val empWalkIns = walkIns.filter { it.employeeId == empId && DateTimeHelper.matchesFilter(it.createdAt, dateFilter) }
            val empSales = sales.filter { it.employeeId == empId && DateTimeHelper.matchesFilter(it.conversionTimestamp, dateFilter) }

            val daysPresent = empAtt.count { it.attendanceStatus != AttendanceStatus.REJECTED }
            val lateCount = empAtt.count { it.attendanceStatus == AttendanceStatus.LATE }
            val closedCount = empWalkIns.count { it.status == LeadStatus.CLOSED }
            val totalWalkIns = empWalkIns.size
            val convRate = if (totalWalkIns > 0) (closedCount.toDouble() / totalWalkIns) * 100.0 else 0.0
            val salesAmount = empSales.sumOf { it.billAmount }
            val pendingFu = empWalkIns.count { it.status == LeadStatus.FOLLOW_UP_REQUIRED }

            EmployeePerformance(
                employeeId = empId,
                employeeName = emp.name,
                branchId = emp.branchId,
                branchName = emp.branchName,
                daysPresent = daysPresent,
                lateArrivals = lateCount,
                attendancePercentage = if (daysPresent + lateCount > 0) 100.0 else 0.0,
                totalWalkIns = totalWalkIns,
                closedWalkIns = closedCount,
                conversionRate = convRate,
                totalSalesAmount = salesAmount,
                pendingFollowUps = pendingFu
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 9. Branch-wise Performance (for Owner)
    val branchPerformanceList: StateFlow<List<BranchPerformance>> = combine(
        repository.branches,
        repository.users,
        repository.attendanceRecords,
        activityBundle,
        selectedDateFilter
    ) { branches, users, att, activity, dateFilter ->
        val walkIns = activity.walkIns
        val sales = activity.sales
        val todayStr = DateTimeHelper.todayDateString()

        branches.map { branch ->
            val branchEmployees = users.filter { it.branchId == branch.branchId && it.role == UserRole.EMPLOYEE }
            val branchTodayAtt = att.filter { it.branchId == branch.branchId && it.date == todayStr }
            val branchWalkIns = walkIns.filter { it.branchId == branch.branchId && DateTimeHelper.matchesFilter(it.createdAt, dateFilter) }
            val branchSales = sales.filter { it.branchId == branch.branchId && DateTimeHelper.matchesFilter(it.conversionTimestamp, dateFilter) }

            val present = branchTodayAtt.count { it.attendanceStatus != AttendanceStatus.REJECTED }
            val late = branchTodayAtt.count { it.attendanceStatus == AttendanceStatus.LATE }
            val totalWk = branchWalkIns.size
            val closed = branchWalkIns.count { it.status == LeadStatus.CLOSED }
            val conv = if (totalWk > 0) (closed.toDouble() / totalWk) * 100.0 else 0.0
            val salesAmt = branchSales.sumOf { it.billAmount }

            BranchPerformance(
                branchId = branch.branchId,
                branchName = branch.branchName,
                employeeCount = branchEmployees.size,
                presentCount = present,
                lateCount = late,
                walkInCount = totalWk,
                closedCount = closed,
                conversionRate = conv,
                totalSalesAmount = salesAmt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Action Functions ---

    fun markAttendance(selfieUri: String, lat: Double, lon: Double) {
        val user = currentUser.value
        val res = repository.markAttendance(user, selfieUri, lat, lon)
        res.onSuccess {
            _userMessage.value = "Attendance marked successfully! Status: ${it.attendanceStatus.label}"
        }.onFailure {
            _userMessage.value = it.message ?: "Attendance failed"
        }
    }

    fun createWalkIn(
        name: String,
        mobile: String,
        category: String,
        model: String,
        budget: Double,
        notes: String
    ) {
        val user = currentUser.value
        val res = repository.createWalkIn(name, mobile, category, model, budget, notes, user)
        res.onSuccess {
            _userMessage.value = "Walk-in lead for ${it.customerName} created!"
            _currentScreen.value = "home"
        }.onFailure {
            _userMessage.value = it.message ?: "Failed to create lead"
        }
    }

    fun addFollowUp(
        walkInId: String,
        method: ContactMethod,
        outcome: String,
        notes: String,
        nextDate: String,
        statusAfter: LeadStatus
    ) {
        val user = currentUser.value
        val res = repository.addFollowUp(walkInId, method, outcome, notes, nextDate, statusAfter, user)
        res.onSuccess {
            _userMessage.value = "Follow-up saved successfully!"
        }.onFailure {
            _userMessage.value = it.message ?: "Failed to save follow-up"
        }
    }

    fun closeSale(
        walkInId: String,
        invoiceNumber: String,
        billAmount: Double,
        saleDate: String,
        productCategory: String,
        productModel: String,
        quantity: Int,
        discount: Double,
        notes: String
    ) {
        val user = currentUser.value
        val res = repository.closeSaleAndLead(
            walkInId, invoiceNumber, billAmount, saleDate,
            productCategory, productModel, quantity, discount, notes, user
        )
        res.onSuccess {
            _userMessage.value = "Sale recorded successfully! Invoice #${it.invoiceNumber}"
            _selectedWalkIn.value = null
        }.onFailure {
            _userMessage.value = it.message ?: "Failed to record sale"
        }
    }

    fun checkDuplicateInvoice(inv: String): Boolean {
        val branchId = currentUser.value.branchId
        return repository.checkDuplicateInvoice(inv, branchId)
    }

    fun updateLeadStatus(walkInId: String, newStatus: LeadStatus) {
        val user = currentUser.value
        val res = repository.updateLeadStatus(walkInId, newStatus, user)
        res.onSuccess {
            _userMessage.value = "Status updated to ${newStatus.label}"
        }.onFailure {
            _userMessage.value = it.message ?: "Failed to update status"
        }
    }

    fun submitCorrection(
        type: CorrectionType,
        targetId: String,
        origVal: String,
        reqVal: String,
        reason: String
    ) {
        val user = currentUser.value
        val res = repository.submitCorrectionRequest(type, targetId, origVal, reqVal, reason, user)
        res.onSuccess {
            _userMessage.value = "Correction request submitted for manager/owner review!"
        }.onFailure {
            _userMessage.value = it.message ?: "Failed to submit correction"
        }
    }

    fun reviewCorrection(requestId: String, status: CorrectionStatus, notes: String) {
        val user = currentUser.value
        val res = repository.reviewCorrectionRequest(requestId, status, notes, user)
        res.onSuccess {
            _userMessage.value = "Correction request marked as ${status.label}."
        }.onFailure {
            _userMessage.value = it.message ?: "Failed to update correction"
        }
    }

    fun updateBranch(branch: Branch) {
        val res = repository.updateBranch(branch)
        res.onSuccess {
            _userMessage.value = "Branch '${branch.branchName}' settings updated."
        }
    }

    fun addBranch(branch: Branch) {
        val res = repository.addBranch(branch)
        res.onSuccess {
            _userMessage.value = "Branch '${branch.branchName}' created successfully."
        }
    }

    fun addUser(newUser: UserProfile) {
        val res = repository.addUser(newUser)
        res.onSuccess {
            _userMessage.value = "User '${newUser.name}' created as ${newUser.role.name}."
        }
    }
}
