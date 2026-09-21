package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DateTimeHelper
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val branches by viewModel.branches.collectAsState()
    val selectedBranchFilter by viewModel.selectedBranchFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val kpi by viewModel.dashboardKpi.collectAsState()
    val branchPerf by viewModel.branchPerformanceList.collectAsState()
    val empPerf by viewModel.employeePerformanceList.collectAsState()
    val corrections by viewModel.scopedCorrectionRequests.collectAsState()
    val auditLogs by viewModel.repository.auditLogs.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allWalkIns by viewModel.scopedWalkIns.collectAsState()
    val allAttendance by viewModel.scopedAttendance.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    // 0: Overview, 1: All Walk-Ins, 2: Live Attendance, 3: 13 Branches & Geofence, 4: Corrections, 5: Staff

    var editingBranch by remember { mutableStateOf<Branch?>(null) }
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var reviewingCorrection by remember { mutableStateOf<CorrectionRequest?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Owner Global Filter Bar: Branch Dropdown (All 13 Branches) & Date Filter
        Surface(
            color = Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Owner Console",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            "Full access to all 13 physical retail branches",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    }
                    RoleBadge(role = UserRole.OWNER)
                }

                Spacer(modifier = Modifier.height(12.dp))

                BranchSelectorDropdown(
                    branches = branches,
                    selectedBranchId = selectedBranchFilter,
                    onSelectBranch = { viewModel.setBranchFilter(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                DateFilterChips(
                    selectedFilter = selectedDateFilter,
                    onSelectFilter = { viewModel.setDateFilter(it) }
                )
            }
        }

        // Navigation Tabs: Pure Focus on Attendance & Walk-Ins
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.White,
            contentColor = BrandBluePrimary,
            edgePadding = 16.dp
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Operations Overview", fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("All Walk-Ins (${allWalkIns.size})", fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Live Attendance (${allAttendance.size})", fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("13 Branches & Geofences", fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 4, onClick = { activeTab = 4 }, text = { Text("Corrections (${corrections.count { it.status == CorrectionStatus.PENDING }})", fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 5, onClick = { activeTab = 5 }, text = { Text("Staff Directory", fontWeight = FontWeight.Bold) })
        }

        when (activeTab) {
            0 -> OwnerAnalyticsTab(
                kpi = kpi,
                branchPerf = branchPerf,
                empPerf = empPerf,
                onDrillDownBranch = { branchId ->
                    viewModel.setBranchFilter(branchId)
                }
            )
            1 -> OwnerAllWalkInsTab(
                walkIns = allWalkIns,
                onStatusChange = { lead, newStatus ->
                    viewModel.updateLeadStatus(lead.walkInId, newStatus)
                }
            )
            2 -> OwnerLiveAttendanceTab(attendanceList = allAttendance)
            3 -> BranchesManagementTab(
                branches = branches,
                onEditBranch = { editingBranch = it },
                onAddNewBranch = { showAddBranchDialog = true }
            )
            4 -> CorrectionsReviewTab(
                corrections = corrections,
                onReview = { reviewingCorrection = it }
            )
            5 -> StaffDirectoryTab(
                users = allUsers,
                branches = branches,
                onAddUser = { showAddUserDialog = true }
            )
        }
    }

    // Branch Edit / Create Dialog (Owner Exclusive Geofence Calibration)
    if (editingBranch != null) {
        BranchConfigDialog(
            branch = editingBranch!!,
            onDismiss = { editingBranch = null },
            onSave = { updated ->
                viewModel.updateBranch(updated)
                editingBranch = null
            }
        )
    }

    if (showAddBranchDialog) {
        BranchConfigDialog(
            branch = Branch(
                branchId = "br_${branches.size + 1}",
                branchName = "Branch ${branches.size + 1}",
                address = "New Store Address",
                latitude = 28.6139,
                longitude = 77.2090,
                geofenceRadius = 100.0
            ),
            onDismiss = { showAddBranchDialog = false },
            onSave = { newBranch ->
                viewModel.addBranch(newBranch)
                showAddBranchDialog = false
            }
        )
    }

    // Correction Review Dialog
    if (reviewingCorrection != null) {
        CorrectionReviewDialog(
            request = reviewingCorrection!!,
            onDismiss = { reviewingCorrection = null },
            onDecide = { status, notes ->
                viewModel.reviewCorrection(reviewingCorrection!!.requestId, status, notes)
                reviewingCorrection = null
            }
        )
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserDialog(
            branches = branches,
            onDismiss = { showAddUserDialog = false },
            onSave = { newUser ->
                viewModel.addUser(newUser)
                showAddUserDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// Tab 0: Executive Overview (Pure Attendance & Walk-Ins Focus)
// -------------------------------------------------------------
@Composable
fun OwnerAnalyticsTab(
    kpi: KpiMetrics,
    branchPerf: List<BranchPerformance>,
    empPerf: List<EmployeePerformance>,
    onDrillDownBranch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Company-Wide Operational Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }

        // Attendance & Walk-In Operational Metrics (NO Sales / Currency Numbers)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Today's Attendance",
                    value = "${String.format("%.0f", kpi.attendancePercentage)}%",
                    subtitle = "${kpi.presentToday} Present • ${kpi.lateToday} Late Arrivals",
                    icon = Icons.Default.EventAvailable,
                    color = BrandBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Customer Walk-Ins",
                    value = "${kpi.totalWalkIns}",
                    subtitle = "${kpi.closed} Closed • ${kpi.interested} Interested",
                    icon = Icons.Default.Groups,
                    color = BrandCyanSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Pending Follow-Ups",
                    value = "${kpi.pendingFollowUps}",
                    subtitle = "${kpi.followUpsDueToday} Due Today • ${kpi.overdueFollowUps} Overdue",
                    icon = Icons.Default.Schedule,
                    color = if (kpi.overdueFollowUps > 0) BrightRed else BrandAmberTertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "On-Time Arrivals",
                    value = "${kpi.presentToday - kpi.lateToday}",
                    subtitle = "Checked in before grace period",
                    icon = Icons.Default.VerifiedUser,
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 13 Branches Breakdown List
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("13 Branches Operations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text("Tap to filter", fontSize = 12.sp, color = Slate600)
            }
        }

        items(branchPerf) { branch ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDrillDownBranch(branch.branchId) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(branch.branchName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                        Surface(
                            color = if (branch.lateCount > 0) BrightRedContainer else StatusSuccessContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (branch.lateCount > 0) "${branch.lateCount} Late" else "All On-Time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (branch.lateCount > 0) BrightRedText else StatusSuccessText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Attendance: ${branch.presentCount} / ${branch.employeeCount} Present", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate800)
                        Text("Walk-Ins: ${branch.walkInCount} (${branch.closedCount} Closed)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                    }
                }
            }
        }

        // Employee Performance Leaderboard (Walk-ins & Attendance Only)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Sales Staff Attendance & Leads Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
        }

        items(empPerf) { emp ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(emp.employeeName, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                        Text("${emp.branchName} • ID: ${emp.employeeId}", fontSize = 12.sp, color = Slate700)
                        Text("Walk-Ins Handled: ${emp.totalWalkIns} (${emp.closedWalkIns} closed)", fontSize = 12.sp, color = BrandBluePrimary, fontWeight = FontWeight.Medium)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("${emp.daysPresent} Days Present", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp)
                        Text(
                            text = if (emp.lateArrivals > 0) "${emp.lateArrivals} Late Arrivals" else "No Late Punches",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (emp.lateArrivals > 0) BrightRed else StatusSuccess
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Tab 1: All Customer Walk-Ins (Owner Real-time Feed)
// -------------------------------------------------------------
@Composable
fun OwnerAllWalkInsTab(
    walkIns: List<WalkInLead>,
    onStatusChange: (WalkInLead, LeadStatus) -> Unit
) {
    var filterStatus by remember { mutableStateOf<LeadStatus?>(null) }
    val context = LocalContext.current

    val displayed = if (filterStatus != null) {
        walkIns.filter { it.status == filterStatus }
    } else {
        walkIns
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Live Customer Walk-In Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        "Instant visibility into walk-ins logged by all branch employees",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            }
        }

        // Status Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("All (${walkIns.size})", fontSize = 11.sp) }
                )
                LeadStatus.values().take(4).forEach { st ->
                    val count = walkIns.count { it.status == st }
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = if (filterStatus == st) null else st },
                        label = { Text("${st.label} ($count)", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (displayed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No walk-in leads found matching this filter.", color = Slate600)
                }
            }
        }

        items(displayed) { lead ->
            var showStatusDropdown by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                lead.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    lead.customerMobile,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBluePrimary,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.customerMobile}"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }

                        Box {
                            LeadStatusBadge(
                                status = lead.status,
                                modifier = Modifier.clickable { showStatusDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showStatusDropdown,
                                onDismissRequest = { showStatusDropdown = false }
                            ) {
                                LeadStatus.values().forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st.label) },
                                        onClick = {
                                            onStatusChange(lead, st)
                                            showStatusDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Product: ${lead.productCategory} • ${lead.productModel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Store: ${lead.branchName}", fontSize = 11.sp, color = Slate700, fontWeight = FontWeight.SemiBold)
                            Text("Logged by: ${lead.employeeName}", fontSize = 11.sp, color = Slate600)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Logged: ${DateTimeHelper.formatDateTime(lead.createdAt)}", fontSize = 11.sp, color = Slate600)
                            if (lead.nextFollowUpTimestamp != null) {
                                Text(
                                    "F/U: ${DateTimeHelper.formatDate(lead.nextFollowUpTimestamp!!)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lead.nextFollowUpTimestamp!! < System.currentTimeMillis()) BrightRed else BrandBluePrimary
                                )
                            }
                        }
                    }

                    if (lead.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Notes: \"${lead.notes}\"",
                            fontSize = 12.sp,
                            color = Slate700,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureWhite, RoundedCornerShape(6.dp))
                                .border(1.dp, Slate200, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Tab 2: Live Staff Attendance Feed (All 13 Branches)
// -------------------------------------------------------------
@Composable
fun OwnerLiveAttendanceTab(
    attendanceList: List<AttendanceRecord>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    "Real-Time Staff Attendance Feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    "All physical check-in punches with live selfie and GPS geofence verification",
                    fontSize = 12.sp,
                    color = Slate600
                )
            }
        }

        if (attendanceList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No attendance records logged for the selected filter.", color = Slate600)
                }
            }
        }

        items(attendanceList) { rec ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rec.selfieUrl.isNotEmpty() && !rec.selfieUrl.startsWith("asset")) {
                            AsyncImage(
                                model = rec.selfieUrl,
                                contentDescription = "Employee Selfie",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Slate600, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rec.employeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                            AttendanceStatusBadge(status = rec.attendanceStatus)
                        }

                        Text("${rec.branchName} • ID: ${rec.employeeId}", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${rec.date} at ${DateTimeHelper.formatTime(rec.timestamp)}",
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        val isGeoVerified = rec.attendanceStatus != AttendanceStatus.REJECTED
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isGeoVerified) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isGeoVerified) StatusSuccess else BrightRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGeoVerified) "Inside Geofence (GPS Verified)" else "Outside Geofence",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isGeoVerified) StatusSuccessText else BrightRed
                            )
                            if (rec.attendanceStatus == AttendanceStatus.LATE) {
                                Text(" • ${rec.minutesLate}m late", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Tab 3: 13 Branches Setup & Geofencing (Owner Exclusive)
// -------------------------------------------------------------
@Composable
fun BranchesManagementTab(
    branches: List<Branch>,
    onEditBranch: (Branch) -> Unit,
    onAddNewBranch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "13 Retail Branch Geofences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        "Only the Business Owner can calibrate store coordinates & radius",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
                Button(
                    onClick = onAddNewBranch,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Branch")
                }
            }
        }

        items(branches) { branch ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(branch.branchName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                                Text(branch.address, fontSize = 12.sp, color = Slate600, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Button(
                            onClick = { onEditBranch(branch) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configure", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("GPS Coordinates & Radius", fontSize = 11.sp, color = Slate600)
                            Text("${branch.latitude}, ${branch.longitude} (±${branch.geofenceRadius.toInt()}m)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Hours & Grace", fontSize = 11.sp, color = Slate600)
                            Text("${branch.openingTime} - ${branch.closingTime} (+${branch.gracePeriodMinutes}m)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Branch Geofence Configuration Dialog (Owner Only)
// -------------------------------------------------------------
@Composable
fun BranchConfigDialog(
    branch: Branch,
    onDismiss: () -> Unit,
    onSave: (Branch) -> Unit
) {
    var name by remember { mutableStateOf(branch.branchName) }
    var address by remember { mutableStateOf(branch.address) }
    var latStr by remember { mutableStateOf(branch.latitude.toString()) }
    var lonStr by remember { mutableStateOf(branch.longitude.toString()) }
    var radiusStr by remember { mutableStateOf(branch.geofenceRadius.toInt().toString()) }
    var openingTime by remember { mutableStateOf(branch.openingTime) }
    var closingTime by remember { mutableStateOf(branch.closingTime) }
    var graceMinutesStr by remember { mutableStateOf(branch.gracePeriodMinutes.toString()) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsStatusMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = BrandBluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure ${branch.branchName}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 440.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isDetectingGps = true
                                val loc = LocationHelper.getCurrentLocation(context)
                                if (loc != null) {
                                    latStr = String.format(java.util.Locale.US, "%.6f", loc.latitude)
                                    lonStr = String.format(java.util.Locale.US, "%.6f", loc.longitude)
                                    gpsStatusMsg = "Coordinates set to your current device location!"
                                } else {
                                    gpsStatusMsg = "Could not obtain current GPS. Ensure location is enabled."
                                }
                                isDetectingGps = false
                            }
                        },
                        enabled = !isDetectingGps,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlueContainer,
                            contentColor = BrandBlueOnContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetching GPS...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set to My Current GPS Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (gpsStatusMsg != null) {
                        Text(
                            text = gpsStatusMsg!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (gpsStatusMsg!!.contains("set to your", ignoreCase = true)) StatusSuccess else BrightRed,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Branch Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Physical Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latStr,
                            onValueChange = { latStr = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lonStr,
                            onValueChange = { lonStr = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Text("Preset Geofence Radius:", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(50, 100, 250, 500, 1000, 5000).forEach { p ->
                            FilterChip(
                                selected = radiusStr == p.toString(),
                                onClick = { radiusStr = p.toString() },
                                label = { Text(if (p == 5000) "5km" else "${p}m", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = radiusStr,
                        onValueChange = { radiusStr = it },
                        label = { Text("Geofence Radius (Meters)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = openingTime,
                            onValueChange = { openingTime = it },
                            label = { Text("Opening (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = graceMinutesStr,
                            onValueChange = { graceMinutesStr = it },
                            label = { Text("Grace (Mins)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = closingTime,
                        onValueChange = { closingTime = it },
                        label = { Text("Closing Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = branch.copy(
                        branchName = name,
                        address = address,
                        latitude = latStr.toDoubleOrNull() ?: branch.latitude,
                        longitude = lonStr.toDoubleOrNull() ?: branch.longitude,
                        geofenceRadius = radiusStr.toDoubleOrNull() ?: branch.geofenceRadius,
                        openingTime = openingTime,
                        closingTime = closingTime,
                        gracePeriodMinutes = graceMinutesStr.toIntOrNull() ?: branch.gracePeriodMinutes
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -------------------------------------------------------------
// Tab 4: Attendance Correction Requests Review Queue
// -------------------------------------------------------------
@Composable
fun CorrectionsReviewTab(
    corrections: List<CorrectionRequest>,
    onReview: (CorrectionRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Correction Requests Review Queue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }

        if (corrections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No correction requests submitted.", color = Slate600)
                }
            }
        } else {
            items(corrections) { req ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                req.requestType.label,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Surface(
                                color = when (req.status) {
                                    CorrectionStatus.PENDING -> StatusWarningContainer
                                    CorrectionStatus.APPROVED -> StatusSuccessContainer
                                    CorrectionStatus.REJECTED -> StatusErrorContainer
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    req.status.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (req.status) {
                                        CorrectionStatus.PENDING -> StatusWarningText
                                        CorrectionStatus.APPROVED -> StatusSuccessText
                                        CorrectionStatus.REJECTED -> StatusErrorText
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Staff: ${req.employeeName} • ${req.branchName}", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.Medium)
                        Text("Original: ${req.originalValue}", fontSize = 12.sp, color = Slate600)
                        Text("Requested Adjustment: ${req.requestedValue}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandBluePrimary)
                        Text("Reason: \"${req.reason}\"", fontSize = 12.sp, color = Slate800)

                        if (req.status == CorrectionStatus.PENDING) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onReview(req) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Review & Decide")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CorrectionReviewDialog(
    request: CorrectionRequest,
    onDismiss: () -> Unit,
    onDecide: (CorrectionStatus, String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decide on Correction Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Staff: ${request.employeeName} (${request.branchName})", fontWeight = FontWeight.SemiBold)
                Text("Original: ${request.originalValue}")
                Text("Requested: ${request.requestedValue}")
                Text("Reason: ${request.reason}")

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reviewer Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onDecide(CorrectionStatus.APPROVED, notes) },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text("Approve")
                }
                Button(
                    onClick = { onDecide(CorrectionStatus.REJECTED, notes) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightRed)
                ) {
                    Text("Reject")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -------------------------------------------------------------
// Tab 5: Staff Directory
// -------------------------------------------------------------
@Composable
fun StaffDirectoryTab(
    users: List<UserProfile>,
    branches: List<Branch>,
    onAddUser: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Retail Staff & Managers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Button(
                    onClick = onAddUser,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Staff")
                }
            }
        }

        items(users) { user ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(user.name, fontWeight = FontWeight.Bold, color = Slate900, fontSize = 15.sp)
                        Text("ID: ${user.employeeId.ifEmpty { user.userId }} • ${user.branchName}", fontSize = 12.sp, color = Slate700)
                        Text(user.phone, fontSize = 12.sp, color = BrandBluePrimary, fontWeight = FontWeight.Medium)
                    }
                    RoleBadge(role = user.role)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    branches: List<Branch>,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var empId by remember { mutableStateOf("EMP-${(100..999).random()}") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91 ") }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var selectedBranch by remember { mutableStateOf(branches.firstOrNull() ?: Branch("br_1", "Connaught Place", "Delhi")) }
    var branchExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Employee to System", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = empId,
                    onValueChange = { empId = it },
                    label = { Text("Employee ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Phone") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role Selection:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(UserRole.EMPLOYEE, UserRole.BRANCH_MANAGER).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }

                Text("Assigned Store Branch:", fontSize = 12.sp, color = Slate700, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = branchExpanded,
                    onExpandedChange = { branchExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBranch.branchName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = branchExpanded,
                        onDismissRequest = { branchExpanded = false }
                    ) {
                        branches.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.branchName) },
                                onClick = {
                                    selectedBranch = b
                                    branchExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newUser = UserProfile(
                        userId = "usr_${System.currentTimeMillis()}",
                        name = name.ifEmpty { "New Staff" },
                        role = role,
                        branchId = selectedBranch.branchId,
                        branchName = selectedBranch.branchName,
                        employeeId = empId,
                        email = email.ifEmpty { "${empId.lowercase()}@retailpulse.com" },
                        phone = phone
                    )
                    onSave(newUser)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
